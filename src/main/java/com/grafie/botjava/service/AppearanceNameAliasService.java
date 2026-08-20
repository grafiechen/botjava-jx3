package com.grafie.botjava.service;

import com.grafie.botjava.entity.AppearanceNameAlias;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.AppearanceNameAliasMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AppearanceNameAliasService {

    public static final String REVIEW_PENDING = "PENDING";
    public static final String REVIEW_APPROVED = "APPROVED";
    public static final String GLOBAL_SCOPE = "__GLOBAL__";
    private static final String JX3API_ACTOR = "JX3API";

    private final AppearanceNameAliasMapper aliasMapper;
    private final AppearanceNamePermissionConfiguration permissionConfiguration;

    public AppearanceNameAliasService(AppearanceNameAliasMapper aliasMapper,
                                      AppearanceNamePermissionConfiguration permissionConfiguration) {
        this.aliasMapper = aliasMapper;
        this.permissionConfiguration = permissionConfiguration;
    }

    @Transactional
    public AddResult add(GroupAtMessageCreateDto message, String canonicalName, String aliasesText, REGEX command) {
        if (!canManage(message, command)) {
            return AddResult.permissionDenied(clean(canonicalName));
        }
        String groupOpenId = requireGroup(message == null ? null : message.getGroupOpenid());
        String cleanedCanonical = requireName(canonicalName, "外观正式名称");
        String normalizedCanonical = normalizeName(cleanedCanonical);
        List<String> aliases = parseAliases(aliasesText);
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("别名不能为空");
        }

        List<String> conflicts = new ArrayList<>();
        List<String> unchanged = new ArrayList<>();
        List<String> created = new ArrayList<>();
        boolean reviewPassed = permissionConfiguration.isAppearanceNameReviewPassed(message, command);
        for (String alias : aliases) {
            String normalizedAlias = normalizeName(alias);
            AppearanceNameAlias existing = aliasMapper.findByGroupOpenIdAndNormalizedAliasName(groupOpenId, normalizedAlias);
            if (existing == null) {
                continue;
            }
            if (normalizedCanonical.equals(existing.getNormalizedCanonicalName())) {
                unchanged.add(alias + displayReviewStatus(existing.getReviewStatus()));
            } else {
                conflicts.add(alias + "->" + existing.getCanonicalName() + displayReviewStatus(existing.getReviewStatus()));
            }
        }
        if (!conflicts.isEmpty()) {
            return AddResult.conflict(cleanedCanonical, conflicts);
        }

        for (String alias : aliases) {
            String normalizedAlias = normalizeName(alias);
            if (aliasMapper.findByGroupOpenIdAndNormalizedAliasName(groupOpenId, normalizedAlias) != null) {
                continue;
            }
            AppearanceNameAlias entity = new AppearanceNameAlias();
            entity.setGroupOpenId(groupOpenId);
            entity.setCanonicalName(cleanedCanonical);
            entity.setNormalizedCanonicalName(normalizedCanonical);
            entity.setAliasName(alias);
            entity.setNormalizedAliasName(normalizedAlias);
            entity.setReviewStatus(reviewPassed ? REVIEW_APPROVED : REVIEW_PENDING);
            entity.setCreatorMemberOpenId(memberOpenId(message));
            entity.setCreatorName(memberName(message));
            if (reviewPassed) {
                entity.setReviewerMemberOpenId(memberOpenId(message));
                entity.setReviewerName(memberName(message));
                entity.setReviewTime(LocalDateTime.now());
            }
            aliasMapper.save(entity);
            created.add(alias);
        }
        return AddResult.added(cleanedCanonical, created, unchanged, reviewPassed);
    }

    @Transactional
    public TrustedImportResult importTrustedAliases(String canonicalName, String aliasesText) {
        String cleanedCanonical = requireName(canonicalName, "外观正式名称");
        String normalizedCanonical = normalizeName(cleanedCanonical);
        List<String> aliases = parseAliases(aliasesText);
        if (aliases.isEmpty()) {
            return new TrustedImportResult(cleanedCanonical, List.of(), List.of(), List.of());
        }

        List<String> created = new ArrayList<>();
        List<String> approved = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String alias : aliases) {
            String normalizedAlias = normalizeName(alias);
            AppearanceNameAlias existing = aliasMapper.findByGroupOpenIdAndNormalizedAliasName(
                    GLOBAL_SCOPE, normalizedAlias);
            if (existing == null) {
                AppearanceNameAlias entity = new AppearanceNameAlias();
                entity.setGroupOpenId(GLOBAL_SCOPE);
                entity.setCanonicalName(cleanedCanonical);
                entity.setNormalizedCanonicalName(normalizedCanonical);
                entity.setAliasName(alias);
                entity.setNormalizedAliasName(normalizedAlias);
                entity.setReviewStatus(REVIEW_APPROVED);
                entity.setCreatorName(JX3API_ACTOR);
                entity.setReviewerName(JX3API_ACTOR);
                entity.setReviewTime(now);
                aliasMapper.save(entity);
                created.add(alias);
                continue;
            }
            if (!normalizedCanonical.equals(existing.getNormalizedCanonicalName())) {
                conflicts.add(alias + "->" + existing.getCanonicalName());
                continue;
            }
            if (!REVIEW_APPROVED.equals(existing.getReviewStatus())) {
                existing.setReviewStatus(REVIEW_APPROVED);
                existing.setReviewerName(JX3API_ACTOR);
                existing.setReviewTime(now);
                aliasMapper.save(existing);
                approved.add(alias);
            }
        }
        return new TrustedImportResult(cleanedCanonical, created, approved, conflicts);
    }

    @Transactional(readOnly = true)
    public List<AliasGroup> list(String groupOpenId) {
        return listByStatus(groupOpenId, REVIEW_APPROVED);
    }

    @Transactional(readOnly = true)
    public PendingListResult pendingList(GroupAtMessageCreateDto message, REGEX command) {
        if (!canManage(message, command)) {
            return PendingListResult.permissionDenied();
        }
        return PendingListResult.allowed(listByStatus(message.getGroupOpenid(), REVIEW_PENDING));
    }

    @Transactional
    public ReviewResult approve(GroupAtMessageCreateDto message, String name, String aliasName, REGEX command) {
        if (!canManage(message, command)) {
            return ReviewResult.permissionDenied(clean(name));
        }
        String groupOpenId = requireGroup(message == null ? null : message.getGroupOpenid());
        String cleanedName = requireName(name, "外观名称");
        String normalizedName = normalizeName(cleanedName);
        List<AppearanceNameAlias> candidates;
        String cleanedAliasName = clean(aliasName);
        if (cleanedAliasName != null) {
            LookupResult target = lookupAny(groupOpenId, cleanedName);
            if (!target.found()) {
                return ReviewResult.notFound(cleanedName);
            }
            candidates = aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameOrderByAliasNameAsc(
                            groupOpenId, normalizeName(target.group().canonicalName()))
                    .stream()
                    .filter(alias -> REVIEW_PENDING.equals(alias.getReviewStatus()))
                    .filter(alias -> normalizeName(cleanedAliasName).equals(alias.getNormalizedAliasName()))
                    .toList();
        } else {
            List<AppearanceNameAlias> rowsByCanonical = aliasMapper
                    .findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                            groupOpenId, normalizedName, REVIEW_PENDING);
            if (!rowsByCanonical.isEmpty()) {
                candidates = rowsByCanonical;
            } else {
                AppearanceNameAlias exactAlias = aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                        groupOpenId, normalizedName, REVIEW_PENDING);
                candidates = exactAlias == null ? List.of() : List.of(exactAlias);
            }
        }
        if (candidates.isEmpty()) {
            return ReviewResult.notFound(cleanedName);
        }
        LocalDateTime now = LocalDateTime.now();
        for (AppearanceNameAlias candidate : candidates) {
            candidate.setReviewStatus(REVIEW_APPROVED);
            candidate.setReviewerMemberOpenId(memberOpenId(message));
            candidate.setReviewerName(memberName(message));
            candidate.setReviewTime(now);
            aliasMapper.save(candidate);
        }
        return ReviewResult.approved(candidates.getFirst().getCanonicalName(), candidates.size());
    }

    @Transactional(readOnly = true)
    public LookupResult lookup(String groupOpenId, String name) {
        String cleanedGroup = requireGroup(groupOpenId);
        String cleanedName = requireName(name, "外观名称");
        String normalizedName = normalizeName(cleanedName);
        AppearanceNameAlias alias = aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                cleanedGroup, normalizedName, REVIEW_APPROVED);
        if (alias != null) {
            return LookupResult.found(cleanedName, alias.getAliasName(), toAliasGroup(cleanedGroup, alias, REVIEW_APPROVED));
        }
        List<AppearanceNameAlias> rows = aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                cleanedGroup, normalizedName, REVIEW_APPROVED);
        if (!rows.isEmpty()) {
            return LookupResult.found(cleanedName, null, toAliasGroup(rows));
        }
        return LookupResult.notFound(cleanedName);
    }

    @Transactional
    public DeleteResult delete(GroupAtMessageCreateDto message, String name, String aliasName, REGEX command) {
        if (!canManage(message, command)) {
            return DeleteResult.permissionDenied(clean(name));
        }
        String groupOpenId = requireGroup(message == null ? null : message.getGroupOpenid());
        String cleanedName = requireName(name, "外观名称");
        LookupResult target = lookupAny(groupOpenId, cleanedName);
        if (!target.found()) {
            return DeleteResult.notFound(cleanedName);
        }
        String normalizedCanonical = normalizeName(target.group().canonicalName());
        String cleanedAlias = clean(aliasName);
        if (cleanedAlias == null) {
            AppearanceNameAlias exactAlias = aliasMapper.findByGroupOpenIdAndNormalizedAliasName(
                    groupOpenId, normalizeName(cleanedName));
            if (exactAlias != null && !exactAlias.getNormalizedAliasName().equals(exactAlias.getNormalizedCanonicalName())) {
                long deleted = aliasMapper.deleteByGroupOpenIdAndNormalizedCanonicalNameAndNormalizedAliasName(
                        groupOpenId, normalizedCanonical, exactAlias.getNormalizedAliasName());
                return deleted > 0 ? DeleteResult.aliasDeleted(target.group().canonicalName(), exactAlias.getAliasName())
                        : DeleteResult.notFound(cleanedName);
            }
            long deleted = aliasMapper.deleteByGroupOpenIdAndNormalizedCanonicalName(groupOpenId, normalizedCanonical);
            return deleted > 0 ? DeleteResult.groupDeleted(target.group().canonicalName(), deleted)
                    : DeleteResult.notFound(cleanedName);
        }
        long deleted = aliasMapper.deleteByGroupOpenIdAndNormalizedCanonicalNameAndNormalizedAliasName(
                groupOpenId, normalizedCanonical, normalizeName(cleanedAlias));
        return deleted > 0 ? DeleteResult.aliasDeleted(target.group().canonicalName(), cleanedAlias)
                : DeleteResult.notFound(cleanedAlias);
    }

    @Transactional(readOnly = true)
    public String resolve(String groupOpenId, String itemName) {
        String cleanedName = clean(itemName);
        if (cleanedName == null || clean(groupOpenId) == null) {
            return itemName;
        }
        LookupResult result = lookup(groupOpenId, cleanedName);
        return result.found() ? result.group().canonicalName() : itemName;
    }

    public String normalizeName(String name) {
        String cleaned = clean(name);
        if (cleaned == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        cleaned.toLowerCase(Locale.ROOT).codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || isSeparator(codePoint)) {
                return;
            }
            normalized.appendCodePoint(codePoint);
        });
        return normalized.toString();
    }

    private List<AliasGroup> listByStatus(String groupOpenId, String reviewStatus) {
        String cleanedGroup = requireGroup(groupOpenId);
        Map<String, AliasGroupBuilder> grouped = new LinkedHashMap<>();
        for (AppearanceNameAlias alias : aliasMapper.findByGroupOpenIdAndReviewStatusOrderByNormalizedCanonicalNameAscAliasNameAsc(
                cleanedGroup, reviewStatus)) {
            grouped.computeIfAbsent(alias.getNormalizedCanonicalName(), key ->
                    new AliasGroupBuilder(alias.getCanonicalName())).aliases().add(alias.getAliasName());
        }
        return grouped.values().stream()
                .map(builder -> new AliasGroup(builder.canonicalName(), builder.aliases()))
                .toList();
    }

    private LookupResult lookupAny(String groupOpenId, String name) {
        String cleanedGroup = requireGroup(groupOpenId);
        String cleanedName = requireName(name, "外观名称");
        String normalizedName = normalizeName(cleanedName);
        AppearanceNameAlias alias = aliasMapper.findByGroupOpenIdAndNormalizedAliasName(cleanedGroup, normalizedName);
        if (alias != null) {
            return LookupResult.found(cleanedName, alias.getAliasName(), toAliasGroup(cleanedGroup, alias, null));
        }
        List<AppearanceNameAlias> rows = aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameOrderByAliasNameAsc(
                cleanedGroup, normalizedName);
        if (!rows.isEmpty()) {
            return LookupResult.found(cleanedName, null, toAliasGroup(rows));
        }
        return LookupResult.notFound(cleanedName);
    }

    private boolean isSeparator(int codePoint) {
        return switch (codePoint) {
            case '·', '.', '。', '-', '_', '—', '－', '《', '》', '〈', '〉', '(', ')', '（', '）', '[', ']', '【', '】' -> true;
            default -> false;
        };
    }

    private AliasGroup toAliasGroup(String groupOpenId, AppearanceNameAlias alias, String reviewStatus) {
        List<AppearanceNameAlias> rows = reviewStatus == null
                ? aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameOrderByAliasNameAsc(
                groupOpenId, alias.getNormalizedCanonicalName())
                : aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                groupOpenId, alias.getNormalizedCanonicalName(), reviewStatus);
        return toAliasGroup(rows.isEmpty() ? List.of(alias) : rows);
    }

    private AliasGroup toAliasGroup(List<AppearanceNameAlias> rows) {
        AppearanceNameAlias first = rows.getFirst();
        return new AliasGroup(first.getCanonicalName(), rows.stream()
                .map(AppearanceNameAlias::getAliasName)
                .toList());
    }

    private List<String> parseAliases(String aliasesText) {
        String cleaned = clean(aliasesText);
        if (cleaned == null) {
            return List.of();
        }
        List<String> aliases = new ArrayList<>();
        for (String part : cleaned.split("[\\s,，、;；/／]+")) {
            String alias = clean(part);
            if (alias != null && alias.length() <= 100 && !aliases.contains(alias)) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    private String displayReviewStatus(String reviewStatus) {
        if (REVIEW_APPROVED.equals(reviewStatus)) {
            return "（已通过）";
        }
        if (REVIEW_PENDING.equals(reviewStatus)) {
            return "（待审核）";
        }
        return "";
    }

    private boolean canManage(GroupAtMessageCreateDto message, REGEX command) {
        return permissionConfiguration.canManageAppearanceName(message, command);
    }

    private String requireGroup(String groupOpenId) {
        return GLOBAL_SCOPE;
    }

    private String requireName(String name, String label) {
        String cleaned = clean(name);
        if (cleaned == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        if (cleaned.length() > 100) {
            throw new IllegalArgumentException(label + "最多 100 个字符");
        }
        return cleaned;
    }

    private String memberOpenId(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        if (author == null) {
            return null;
        }
        return firstNotBlank(author.getMemberOpenid(), author.getId());
    }

    private String memberName(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        String username = author == null ? null : clean(author.getUsername());
        return username == null ? firstNotBlank(memberOpenId(message), "未知群员") : username;
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

    private record AliasGroupBuilder(String canonicalName, List<String> aliases) {
        private AliasGroupBuilder(String canonicalName) {
            this(canonicalName, new ArrayList<>());
        }
    }

    public record TrustedImportResult(String canonicalName, List<String> createdAliases,
                                      List<String> approvedAliases, List<String> conflicts) {
        public TrustedImportResult {
            createdAliases = createdAliases == null ? List.of() : List.copyOf(createdAliases);
            approvedAliases = approvedAliases == null ? List.of() : List.copyOf(approvedAliases);
            conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        }
    }

    public record AliasGroup(String canonicalName, List<String> aliases) {
        public AliasGroup {
            aliases = aliases == null ? List.of() : List.copyOf(new ArrayList<>(aliases));
        }
    }

    public record AddResult(Status status, String canonicalName, List<String> aliases, List<String> unchangedAliases,
                            List<String> conflicts, boolean reviewPassed) {
        public static AddResult added(String canonicalName, List<String> aliases, List<String> unchangedAliases) {
            return added(canonicalName, aliases, unchangedAliases, true);
        }

        public static AddResult added(String canonicalName, List<String> aliases, List<String> unchangedAliases,
                                      boolean reviewPassed) {
            return new AddResult(Status.ADDED, canonicalName, aliases, unchangedAliases, List.of(), reviewPassed);
        }

        public static AddResult conflict(String canonicalName, List<String> conflicts) {
            return new AddResult(Status.CONFLICT, canonicalName, List.of(), List.of(), conflicts, false);
        }

        public static AddResult permissionDenied(String canonicalName) {
            return new AddResult(Status.PERMISSION_DENIED, canonicalName, List.of(), List.of(), List.of(), false);
        }

        public enum Status {
            ADDED,
            CONFLICT,
            PERMISSION_DENIED
        }
    }

    public record LookupResult(boolean found, String queryName, String matchedAlias, AliasGroup group) {
        public static LookupResult found(String queryName, String matchedAlias, AliasGroup group) {
            return new LookupResult(true, queryName, matchedAlias, group);
        }

        public static LookupResult notFound(String queryName) {
            return new LookupResult(false, queryName, null, null);
        }
    }

    public record PendingListResult(Status status, List<AliasGroup> groups) {
        public static PendingListResult allowed(List<AliasGroup> groups) {
            return new PendingListResult(Status.ALLOWED, groups);
        }

        public static PendingListResult permissionDenied() {
            return new PendingListResult(Status.PERMISSION_DENIED, List.of());
        }

        public enum Status {
            ALLOWED,
            PERMISSION_DENIED
        }
    }

    public record ReviewResult(Status status, String canonicalName, int approvedCount) {
        public static ReviewResult approved(String canonicalName, int approvedCount) {
            return new ReviewResult(Status.APPROVED, canonicalName, approvedCount);
        }

        public static ReviewResult notFound(String name) {
            return new ReviewResult(Status.NOT_FOUND, name, 0);
        }

        public static ReviewResult permissionDenied(String name) {
            return new ReviewResult(Status.PERMISSION_DENIED, name, 0);
        }

        public enum Status {
            APPROVED,
            NOT_FOUND,
            PERMISSION_DENIED
        }
    }

    public record DeleteResult(Status status, String canonicalName, String aliasName, long deletedCount) {
        public static DeleteResult groupDeleted(String canonicalName, long deletedCount) {
            return new DeleteResult(Status.GROUP_DELETED, canonicalName, null, deletedCount);
        }

        public static DeleteResult aliasDeleted(String canonicalName, String aliasName) {
            return new DeleteResult(Status.ALIAS_DELETED, canonicalName, aliasName, 1);
        }

        public static DeleteResult notFound(String name) {
            return new DeleteResult(Status.NOT_FOUND, name, null, 0);
        }

        public static DeleteResult permissionDenied(String name) {
            return new DeleteResult(Status.PERMISSION_DENIED, name, null, 0);
        }

        public enum Status {
            GROUP_DELETED,
            ALIAS_DELETED,
            NOT_FOUND,
            PERMISSION_DENIED
        }
    }
}
