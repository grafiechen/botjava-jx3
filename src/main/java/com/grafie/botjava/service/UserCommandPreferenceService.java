package com.grafie.botjava.service;

import com.grafie.botjava.entity.UserInfo;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.mapper.UserInfoMapper;
import com.grafie.botjava.mapper.UserRoleBindingMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 保存用户默认角色，并在执行查询前补充缺省参数。
 */
@Service
public class UserCommandPreferenceService {
    private final UserInfoMapper userInfoMapper;
    private final UserRoleBindingMapper roleBindingMapper;

    public UserCommandPreferenceService(UserInfoMapper userInfoMapper, UserRoleBindingMapper roleBindingMapper) {
        this.userInfoMapper = userInfoMapper;
        this.roleBindingMapper = roleBindingMapper;
    }

    public CommandArguments applyDefaults(GroupAtMessageCreateDto message, CommandArguments arguments) {
        String memberOpenId = memberOpenId(message);
        if (memberOpenId == null) {
            return arguments;
        }
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        return userInfo == null ? arguments
                : arguments.withDefaults(userInfo.getServer(), userInfo.getRoleName(), userInfo.getSchool());
    }

    public UserInfo find(GroupAtMessageCreateDto message) {
        return userInfoMapper.findByMemberOpenId(requireMemberOpenId(message));
    }

    @Transactional
    public UserInfo bindSchool(GroupAtMessageCreateDto message, String school) {
        String memberOpenId = requireMemberOpenId(message);
        String cleanedSchool = requireText(school, "门派");
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setMemberOpenId(memberOpenId);
        }
        userInfo.setSchool(cleanedSchool);
        return userInfoMapper.save(userInfo);
    }

    @Transactional
    public boolean unbindSchool(GroupAtMessageCreateDto message) {
        String memberOpenId = requireMemberOpenId(message);
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        if (userInfo == null || clean(userInfo.getSchool()) == null) {
            return false;
        }
        userInfo.setSchool(null);
        if (hasAnyRole(userInfo)) {
            userInfoMapper.save(userInfo);
        } else {
            userInfoMapper.delete(userInfo);
        }
        return true;
    }

    @Transactional
    public UserInfo bind(GroupAtMessageCreateDto message, String server, String roleName) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        saveRole(groupOpenId, memberOpenId, cleanedServer, cleanedRoleName);
        return saveDefaultRole(memberOpenId, cleanedServer, cleanedRoleName);
    }

    @Transactional
    public UserRoleBinding addRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        UserRoleBinding binding = saveRole(groupOpenId, memberOpenId, cleanedServer, cleanedRoleName);
        if (userInfoMapper.findByMemberOpenId(memberOpenId) == null) {
            saveDefaultRole(memberOpenId, cleanedServer, cleanedRoleName);
        }
        return binding;
    }

    @Transactional
    public UserRoleBinding updateRoleSchool(GroupAtMessageCreateDto message,
                                            String server, String roleName, String school) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        String cleanedSchool = requireText(school, "门派");
        UserRoleBinding binding = roleBindingMapper.findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName(
                groupOpenId, memberOpenId, cleanedServer, cleanedRoleName);
        if (binding == null) {
            throw new IllegalArgumentException("未找到该角色，请先使用：我的角色");
        }
        UserInfo defaultRole = userInfoMapper.findByMemberOpenId(memberOpenId);
        boolean updatesDefault = sameRole(binding, defaultRole);
        binding.setSchool(cleanedSchool);
        UserRoleBinding saved = roleBindingMapper.save(binding);
        if (updatesDefault) {
            saveDefault(memberOpenId, cleanedServer, cleanedRoleName, cleanedSchool);
        }
        return saved;
    }

    public BindingSnapshot findBindings(GroupAtMessageCreateDto message) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        UserInfo defaultRole = userInfoMapper.findByMemberOpenId(memberOpenId);
        List<UserRoleBinding> storedRoles = roleBindingMapper
                .findByGroupOpenIdAndMemberOpenIdOrderByIdAsc(groupOpenId, memberOpenId);
        List<UserRoleBinding> roles = new ArrayList<>(storedRoles == null ? Collections.emptyList() : storedRoles);
        return new BindingSnapshot(defaultRole, List.copyOf(roles));
    }

    public List<UserRoleBinding> findGroupBindings(String groupOpenId) {
        String cleanedGroupOpenId = requireText(groupOpenId, "群标识");
        List<UserRoleBinding> bindings = roleBindingMapper
                .findByGroupOpenIdOrderByMemberOpenIdAscIdAsc(cleanedGroupOpenId);
        return bindings == null ? List.of() : List.copyOf(bindings);
    }

    @Transactional
    public boolean deleteRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        UserRoleBinding target = roleBindingMapper.findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName(
                groupOpenId, memberOpenId, cleanedServer, cleanedRoleName);
        if (target == null) {
            return false;
        }
        UserInfo defaultRole = userInfoMapper.findByMemberOpenId(memberOpenId);
        boolean removesDefault = sameRole(target, defaultRole);
        roleBindingMapper.delete(target);
        if (removesDefault) {
            List<UserRoleBinding> remaining = roleBindingMapper
                    .findByGroupOpenIdAndMemberOpenIdOrderByIdAsc(groupOpenId, memberOpenId)
                    .stream()
                    .filter(role -> !java.util.Objects.equals(role.getId(), target.getId()))
                    .toList();
            if (remaining.isEmpty()) {
                clearDefaultRole(defaultRole);
            } else {
                UserRoleBinding next = remaining.getFirst();
                saveDefaultRole(memberOpenId, next.getServer(), next.getRoleName());
            }
        }
        return true;
    }

    @Transactional
    public boolean unbind(GroupAtMessageCreateDto message) {
        String groupOpenId = requireGroupOpenId(message);
        String memberOpenId = requireMemberOpenId(message);
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        long deletedRoles = roleBindingMapper.deleteByGroupOpenIdAndMemberOpenId(groupOpenId, memberOpenId);
        boolean hadDefaultRole = hasAnyRole(userInfo);
        if (hadDefaultRole) {
            clearDefaultRole(userInfo);
        }
        return hadDefaultRole || deletedRoles > 0;
    }

    private UserInfo saveDefault(String memberOpenId, String server, String roleName, String school) {
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setMemberOpenId(memberOpenId);
        }
        userInfo.setServer(server);
        userInfo.setRoleName(roleName);
        userInfo.setSchool(school);
        return userInfoMapper.save(userInfo);
    }

    private UserInfo saveDefaultRole(String memberOpenId, String server, String roleName) {
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setMemberOpenId(memberOpenId);
        }
        userInfo.setServer(server);
        userInfo.setRoleName(roleName);
        return userInfoMapper.save(userInfo);
    }

    private void clearDefaultRole(UserInfo userInfo) {
        if (userInfo != null) {
            userInfoMapper.delete(userInfo);
        }
    }

    private UserRoleBinding saveRole(String groupOpenId, String memberOpenId,
                                     String server, String roleName) {
        UserRoleBinding existing = roleBindingMapper.findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName(
                groupOpenId, memberOpenId, server, roleName);
        if (existing != null) {
            return existing;
        }
        UserRoleBinding binding = new UserRoleBinding();
        binding.setGroupOpenId(groupOpenId);
        binding.setMemberOpenId(memberOpenId);
        binding.setRoleName(roleName);
        binding.setServer(server);
        return roleBindingMapper.save(binding);
    }

    private boolean sameRole(UserRoleBinding binding, UserInfo defaultRole) {
        return binding != null && defaultRole != null
                && java.util.Objects.equals(binding.getServer(), defaultRole.getServer())
                && java.util.Objects.equals(binding.getRoleName(), defaultRole.getRoleName());
    }

    private boolean hasAnyRole(UserInfo binding) {
        return binding != null && (clean(binding.getServer()) != null || clean(binding.getRoleName()) != null);
    }

    private String requireGroupOpenId(GroupAtMessageCreateDto message) {
        String groupOpenId = message == null ? null : clean(firstNotBlank(
                message.getGroupOpenid(), message.getGroupId()));
        if (groupOpenId == null) {
            throw new IllegalArgumentException("当前消息缺少群标识，无法保存绑定。");
        }
        return groupOpenId;
    }

    private String requireMemberOpenId(GroupAtMessageCreateDto message) {
        String memberOpenId = memberOpenId(message);
        if (memberOpenId == null) {
            throw new IllegalArgumentException("当前消息缺少用户标识，无法保存绑定。");
        }
        return memberOpenId;
    }

    private String memberOpenId(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        return author == null ? null : clean(firstNotBlank(author.getMemberOpenid(), author.getId()));
    }

    private String requireText(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(fieldName + "不能为空。");
        }
        return cleaned;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (clean(value) != null) {
                return value;
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

    public record BindingSnapshot(UserInfo defaultRole, List<UserRoleBinding> roles) {
    }
}