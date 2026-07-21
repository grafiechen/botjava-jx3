package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.UserRoleBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleBindingMapper extends JpaRepository<UserRoleBinding, Long> {

    List<UserRoleBinding> findByMemberOpenIdOrderByIdAsc(String memberOpenId);

    UserRoleBinding findByMemberOpenIdAndServerAndRoleName(
            String memberOpenId, String server, String roleName);

    long deleteByMemberOpenId(String memberOpenId);
}
