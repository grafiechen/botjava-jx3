package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.UserRoleBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleBindingMapper extends JpaRepository<UserRoleBinding, Long> {
    List<UserRoleBinding> findByGroupOpenIdAndMemberOpenIdOrderByIdAsc(String groupOpenId, String memberOpenId);

    List<UserRoleBinding> findByGroupOpenIdOrderByMemberOpenIdAscIdAsc(String groupOpenId);

    UserRoleBinding findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName(
            String groupOpenId, String memberOpenId, String server, String roleName);

    long deleteByGroupOpenIdAndMemberOpenId(String groupOpenId, String memberOpenId);
}