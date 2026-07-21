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
        if (userInfo == null) {
            return arguments;
        }
        return arguments.withDefaults(userInfo.getServer(), userInfo.getRoleName(), userInfo.getSchool());
    }

    public UserInfo find(GroupAtMessageCreateDto message) {
        String memberOpenId = requireMemberOpenId(message);
        return userInfoMapper.findByMemberOpenId(memberOpenId);
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
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        saveRole(memberOpenId, cleanedServer, cleanedRoleName);
        return saveDefault(memberOpenId, cleanedServer, cleanedRoleName);
    }

    @Transactional
    public UserRoleBinding addRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        UserRoleBinding binding = saveRole(memberOpenId, cleanedServer, cleanedRoleName);
        if (userInfoMapper.findByMemberOpenId(memberOpenId) == null) {
            saveDefault(memberOpenId, cleanedServer, cleanedRoleName);
        }
        return binding;
    }

    @Transactional
    public UserInfo switchRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        UserRoleBinding binding = roleBindingMapper.findByMemberOpenIdAndServerAndRoleName(
                memberOpenId, cleanedServer, cleanedRoleName);
        if (binding == null) {
            throw new IllegalArgumentException("未找到该角色绑定，请先使用：添加角色 服务器 角色名");
        }
        return saveDefault(memberOpenId, cleanedServer, cleanedRoleName);
    }

    public BindingSnapshot findBindings(GroupAtMessageCreateDto message) {
        String memberOpenId = requireMemberOpenId(message);
        UserInfo defaultRole = userInfoMapper.findByMemberOpenId(memberOpenId);
        List<UserRoleBinding> storedRoles = roleBindingMapper.findByMemberOpenIdOrderByIdAsc(memberOpenId);
        List<UserRoleBinding> roles = new ArrayList<>(storedRoles == null ? Collections.emptyList() : storedRoles);
        if (hasCompleteRole(defaultRole) && roles.stream().noneMatch(role -> sameRole(
                role, defaultRole.getServer(), defaultRole.getRoleName()))) {
            UserRoleBinding legacyRole = new UserRoleBinding();
            legacyRole.setMemberOpenId(memberOpenId);
            legacyRole.setServer(defaultRole.getServer());
            legacyRole.setRoleName(defaultRole.getRoleName());
            roles.add(0, legacyRole);
        }
        return new BindingSnapshot(defaultRole, List.copyOf(roles));
    }

    @Transactional
    public boolean unbindRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String memberOpenId = requireMemberOpenId(message);
        String cleanedServer = requireText(server, "服务器");
        String cleanedRoleName = requireText(roleName, "角色名");
        UserInfo defaultRole = userInfoMapper.findByMemberOpenId(memberOpenId);
        UserRoleBinding target = roleBindingMapper.findByMemberOpenIdAndServerAndRoleName(
                memberOpenId, cleanedServer, cleanedRoleName);
        boolean removesDefault = sameRole(defaultRole, cleanedServer, cleanedRoleName);
        if (target == null && !removesDefault) {
            return false;
        }
        if (target != null) {
            roleBindingMapper.delete(target);
        }
        if (removesDefault) {
            List<UserRoleBinding> remaining = roleBindingMapper.findByMemberOpenIdOrderByIdAsc(memberOpenId)
                    .stream()
                    .filter(role -> !sameRole(role, cleanedServer, cleanedRoleName))
                    .toList();
            if (remaining.isEmpty()) {
                clearDefaultRole(defaultRole);
            } else {
                UserRoleBinding next = remaining.get(0);
                saveDefault(memberOpenId, next.getServer(), next.getRoleName());
            }
        }
        return true;
    }

    private UserInfo saveDefault(String memberOpenId, String server, String roleName) {
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setMemberOpenId(memberOpenId);
        }
        userInfo.setServer(server);
        userInfo.setRoleName(roleName);
        return userInfoMapper.save(userInfo);
    }

    @Transactional
    public boolean unbind(GroupAtMessageCreateDto message) {
        String memberOpenId = requireMemberOpenId(message);
        UserInfo userInfo = userInfoMapper.findByMemberOpenId(memberOpenId);
        long deletedRoles = roleBindingMapper.deleteByMemberOpenId(memberOpenId);
        boolean hadDefaultRole = hasAnyRole(userInfo);
        if (hadDefaultRole) {
            clearDefaultRole(userInfo);
        }
        return hadDefaultRole || deletedRoles > 0;
    }

    private void clearDefaultRole(UserInfo userInfo) {
        userInfo.setServer(null);
        userInfo.setRoleName(null);
        if (clean(userInfo.getSchool()) == null) {
            userInfoMapper.delete(userInfo);
        } else {
            userInfoMapper.save(userInfo);
        }
    }

    private UserRoleBinding saveRole(String memberOpenId, String server, String roleName) {
        UserRoleBinding existing = roleBindingMapper.findByMemberOpenIdAndServerAndRoleName(
                memberOpenId, server, roleName);
        if (existing != null) {
            return existing;
        }
        UserRoleBinding binding = new UserRoleBinding();
        binding.setMemberOpenId(memberOpenId);
        binding.setServer(server);
        binding.setRoleName(roleName);
        return roleBindingMapper.save(binding);
    }

    private boolean sameRole(UserRoleBinding binding, String server, String roleName) {
        return binding != null && server != null && roleName != null
                && server.equals(binding.getServer()) && roleName.equals(binding.getRoleName());
    }

    private boolean sameRole(UserInfo binding, String server, String roleName) {
        return binding != null && server != null && roleName != null
                && server.equals(binding.getServer()) && roleName.equals(binding.getRoleName());
    }

    private boolean hasCompleteRole(UserInfo binding) {
        return binding != null && clean(binding.getServer()) != null && clean(binding.getRoleName()) != null;
    }

    private boolean hasAnyRole(UserInfo binding) {
        return binding != null && (clean(binding.getServer()) != null || clean(binding.getRoleName()) != null);
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
        if (author == null) {
            return null;
        }
        return clean(firstNotBlank(author.getMemberOpenid(), author.getId()));
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
