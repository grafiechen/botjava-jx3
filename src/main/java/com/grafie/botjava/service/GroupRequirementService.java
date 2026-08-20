package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupRequirement;
import com.grafie.botjava.entity.GroupRequirementItem;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.mapper.GroupRequirementItemMapper;
import com.grafie.botjava.mapper.GroupRequirementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupRequirementService {

    private final GroupRequirementMapper requirementMapper;
    private final GroupRequirementItemMapper itemMapper;

    public GroupRequirementService(GroupRequirementMapper requirementMapper,
                                   GroupRequirementItemMapper itemMapper) {
        this.requirementMapper = requirementMapper;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public List<RequirementSummary> list(String groupOpenId) {
        return requirementMapper.findByGroupOpenIdOrderByCreateTimeAscIdAsc(requireGroup(groupOpenId))
                .stream()
                .map(requirement -> new RequirementSummary(
                        requirement.getTitle(),
                        itemMapper.countByRequirement_Id(requirement.getId())))
                .toList();
    }

    @Transactional
    public CreateResult create(GroupAtMessageCreateDto message, String title) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedTitle = requireTitle(title);
        GroupRequirement existing = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedTitle);
        if (existing != null) {
            return new CreateResult(false, cleanedTitle);
        }
        GroupRequirement requirement = new GroupRequirement();
        requirement.setGroupOpenId(groupOpenId);
        requirement.setTitle(cleanedTitle);
        requirement.setCreatorMemberOpenId(requireMemberOpenId(message));
        requirement.setCreatorName(memberName(message));
        requirementMapper.save(requirement);
        return new CreateResult(true, cleanedTitle);
    }

    @Transactional
    public RenameResult rename(GroupAtMessageCreateDto message, String oldTitle, String newTitle) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedOldTitle = requireTitle(oldTitle);
        String cleanedNewTitle = requireTitle(newTitle);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedOldTitle);
        if (requirement == null) {
            return RenameResult.notFound(cleanedOldTitle);
        }
        if (!canManageRequirement(message, requirement)) {
            return RenameResult.notOwner(cleanedOldTitle);
        }
        if (!cleanedOldTitle.equals(cleanedNewTitle)
                && requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedNewTitle) != null) {
            return RenameResult.duplicated(cleanedOldTitle, cleanedNewTitle);
        }
        requirement.setTitle(cleanedNewTitle);
        requirementMapper.save(requirement);
        return RenameResult.renamed(cleanedOldTitle, cleanedNewTitle);
    }

    @Transactional
    public StatusResult updateStatus(GroupAtMessageCreateDto message, String title, String itemContent, String status) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedTitle = requireTitle(title);
        String cleanedItemContent = requireItemContent(itemContent);
        String cleanedStatus = requireStatus(status);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedTitle);
        if (requirement == null) {
            return StatusResult.notFound(cleanedTitle, cleanedItemContent, cleanedStatus);
        }
        if (!canManageRequirement(message, requirement)) {
            return StatusResult.notOwner(cleanedTitle, cleanedItemContent, cleanedStatus);
        }
        List<GroupRequirementItem> matchedItems = itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(requirement.getId())
                .stream()
                .filter(item -> cleanedItemContent.equals(clean(item.getContent())))
                .toList();
        if (matchedItems.isEmpty()) {
            return StatusResult.itemNotFound(cleanedTitle, cleanedItemContent, cleanedStatus);
        }
        if (matchedItems.size() > 1) {
            return StatusResult.duplicated(cleanedTitle, cleanedItemContent, cleanedStatus);
        }
        GroupRequirementItem item = matchedItems.getFirst();
        item.setStatus(cleanedStatus);
        itemMapper.save(item);
        return StatusResult.updated(cleanedTitle, cleanedItemContent, cleanedStatus);
    }

    @Transactional
    public AppendResult append(GroupAtMessageCreateDto message, String title, String content) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedTitle = requireTitle(title);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedTitle);
        if (requirement == null) {
            return new AppendResult(false, cleanedTitle, 0);
        }
        GroupRequirementItem item = new GroupRequirementItem();
        item.setRequirement(requirement);
        item.setMemberOpenId(memberOpenId(message));
        item.setMemberName(memberName(message));
        item.setContent(clean(content));
        itemMapper.save(item);
        long count = itemMapper.countByRequirement_Id(requirement.getId());
        return new AppendResult(true, cleanedTitle, count);
    }

    @Transactional
    public CancelResult cancelOwnItem(GroupAtMessageCreateDto message, String title, int itemIndex) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedTitle = requireTitle(title);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedTitle);
        if (requirement == null) {
            return CancelResult.notFound(cleanedTitle, itemIndex);
        }
        if (itemIndex < 1) {
            return CancelResult.itemNotFound(cleanedTitle, itemIndex);
        }
        List<GroupRequirementItem> items = itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(requirement.getId());
        if (itemIndex > items.size()) {
            return CancelResult.itemNotFound(cleanedTitle, itemIndex);
        }
        GroupRequirementItem item = items.get(itemIndex - 1);
        if (!canCancelRequirementItem(message, requirement, item)) {
            return CancelResult.notOwner(cleanedTitle, itemIndex);
        }
        itemMapper.delete(item);
        return CancelResult.canceled(cleanedTitle, itemIndex);
    }

    @Transactional
    public DeleteResult deleteOwnRequirement(GroupAtMessageCreateDto message, String title) {
        String groupOpenId = requireGroup(message.getGroupOpenid());
        String cleanedTitle = requireTitle(title);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(groupOpenId, cleanedTitle);
        if (requirement == null) {
            return DeleteResult.notFound(cleanedTitle);
        }
        if (!canManageRequirement(message, requirement)) {
            return DeleteResult.notOwner(cleanedTitle);
        }
        itemMapper.deleteByRequirement_Id(requirement.getId());
        requirementMapper.delete(requirement);
        return DeleteResult.deleted(cleanedTitle);
    }

    @Transactional(readOnly = true)
    public RequirementDetail detail(String groupOpenId, String title) {
        String cleanedTitle = requireTitle(title);
        GroupRequirement requirement = requirementMapper.findByGroupOpenIdAndTitle(
                requireGroup(groupOpenId), cleanedTitle);
        if (requirement == null) {
            return null;
        }
        List<RequirementItemView> items = itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(requirement.getId())
                .stream()
                .map(item -> new RequirementItemView(item.getMemberName(), item.getContent(), item.getStatus()))
                .toList();
        return new RequirementDetail(requirement.getTitle(), items);
    }

    private String requireGroup(String groupOpenId) {
        String cleaned = clean(groupOpenId);
        if (cleaned == null) {
            throw new IllegalArgumentException("群 ID 不能为空");
        }
        return cleaned;
    }

    private String requireTitle(String title) {
        String cleaned = clean(title);
        if (cleaned == null) {
            throw new IllegalArgumentException("需求名称不能为空");
        }
        if (cleaned.length() > 80) {
            throw new IllegalArgumentException("需求名称最多 80 个字符");
        }
        return cleaned;
    }

    private String requireItemContent(String itemContent) {
        String cleaned = clean(itemContent);
        if (cleaned == null) {
            throw new IllegalArgumentException("追加内容不能为空");
        }
        if (cleaned.length() > 1000) {
            throw new IllegalArgumentException("追加内容最多 1000 个字符");
        }
        return cleaned;
    }

    private String requireStatus(String status) {
        String cleaned = clean(status);
        if (cleaned == null) {
            throw new IllegalArgumentException("需求状态不能为空");
        }
        if (cleaned.length() > 100) {
            throw new IllegalArgumentException("需求状态最多 100 个字符");
        }
        return cleaned;
    }

    private String memberOpenId(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        return author == null ? null : firstNotBlank(author.getMemberOpenid(), author.getId());
    }

    private String requireMemberOpenId(GroupAtMessageCreateDto message) {
        String memberOpenId = memberOpenId(message);
        if (memberOpenId == null) {
            throw new IllegalArgumentException("无法识别当前操作人");
        }
        return memberOpenId;
    }

    private String memberName(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        String username = author == null ? null : clean(author.getUsername());
        String memberOpenId = memberOpenId(message);
        return username == null ? firstNotBlank(memberOpenId, "未知群员") : username;
    }

    private boolean canManageGroup(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        return author != null && author.canManageGroup();
    }

    private boolean canManageRequirement(GroupAtMessageCreateDto message, GroupRequirement requirement) {
        String memberOpenId = requireMemberOpenId(message);
        return memberOpenId.equals(requirement.getCreatorMemberOpenId()) || canManageGroup(message);
    }

    private boolean canCancelRequirementItem(GroupAtMessageCreateDto message, GroupRequirement requirement,
                                             GroupRequirementItem item) {
        String memberOpenId = requireMemberOpenId(message);
        return memberOpenId.equals(item.getMemberOpenId())
                || memberOpenId.equals(requirement.getCreatorMemberOpenId())
                || canManageGroup(message);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public record RequirementSummary(String title, long itemCount) {
    }

    public record CreateResult(boolean created, String title) {
    }

    public record AppendResult(boolean appended, String title, long itemCount) {
    }

    public record StatusResult(Status status, String title, String itemContent, String itemStatus) {
        public static StatusResult updated(String title, String itemContent, String itemStatus) {
            return new StatusResult(Status.UPDATED, title, itemContent, itemStatus);
        }

        public static StatusResult notFound(String title, String itemContent, String itemStatus) {
            return new StatusResult(Status.NOT_FOUND, title, itemContent, itemStatus);
        }

        public static StatusResult itemNotFound(String title, String itemContent, String itemStatus) {
            return new StatusResult(Status.ITEM_NOT_FOUND, title, itemContent, itemStatus);
        }

        public static StatusResult duplicated(String title, String itemContent, String itemStatus) {
            return new StatusResult(Status.DUPLICATED, title, itemContent, itemStatus);
        }

        public static StatusResult notOwner(String title, String itemContent, String itemStatus) {
            return new StatusResult(Status.NOT_OWNER, title, itemContent, itemStatus);
        }

        public enum Status {
            UPDATED,
            NOT_FOUND,
            ITEM_NOT_FOUND,
            DUPLICATED,
            NOT_OWNER
        }
    }

    public record CancelResult(Status status, String title, int itemIndex) {
        public static CancelResult canceled(String title, int itemIndex) {
            return new CancelResult(Status.CANCELED, title, itemIndex);
        }

        public static CancelResult notFound(String title, int itemIndex) {
            return new CancelResult(Status.REQUIREMENT_NOT_FOUND, title, itemIndex);
        }

        public static CancelResult itemNotFound(String title, int itemIndex) {
            return new CancelResult(Status.ITEM_NOT_FOUND, title, itemIndex);
        }

        public static CancelResult notOwner(String title, int itemIndex) {
            return new CancelResult(Status.NOT_OWNER, title, itemIndex);
        }

        public enum Status {
            CANCELED,
            REQUIREMENT_NOT_FOUND,
            ITEM_NOT_FOUND,
            NOT_OWNER
        }
    }

    public record RenameResult(Status status, String oldTitle, String newTitle) {
        public static RenameResult notFound(String oldTitle) {
            return new RenameResult(Status.NOT_FOUND, oldTitle, null);
        }

        public static RenameResult notOwner(String oldTitle) {
            return new RenameResult(Status.NOT_OWNER, oldTitle, null);
        }

        public static RenameResult duplicated(String oldTitle, String newTitle) {
            return new RenameResult(Status.DUPLICATED, oldTitle, newTitle);
        }

        public static RenameResult renamed(String oldTitle, String newTitle) {
            return new RenameResult(Status.RENAMED, oldTitle, newTitle);
        }

        public enum Status {
            RENAMED,
            NOT_FOUND,
            NOT_OWNER,
            DUPLICATED
        }
    }

    public record DeleteResult(Status status, String title) {
        public static DeleteResult notFound(String title) {
            return new DeleteResult(Status.NOT_FOUND, title);
        }

        public static DeleteResult notOwner(String title) {
            return new DeleteResult(Status.NOT_OWNER, title);
        }

        public static DeleteResult deleted(String title) {
            return new DeleteResult(Status.DELETED, title);
        }

        public enum Status {
            DELETED,
            NOT_FOUND,
            NOT_OWNER
        }
    }

    public record RequirementDetail(String title, List<RequirementItemView> items) {
        public RequirementDetail {
            items = items == null ? List.of() : List.copyOf(new ArrayList<>(items));
        }
    }

    public record RequirementItemView(String memberName, String content, String status) {
    }
}
