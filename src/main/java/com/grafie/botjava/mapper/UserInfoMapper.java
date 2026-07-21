package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInfoMapper extends JpaRepository<UserInfo, Long> {
    UserInfo findByMemberOpenId(String memberOpenId);
}
