package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author grafie.chen
 * @since 2025/1/22  16:24
 */
public interface GroupInfoMapper extends JpaRepository<GroupInfo, Long> {
    /**
     * 根据openGroupId查询
     *
     * @param openGroupId open_group_id
     * @return entity
     */
    GroupInfo findByOpenGroupId(String openGroupId);
}
