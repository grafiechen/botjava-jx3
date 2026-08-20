package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.PushEventReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface PushEventReceiptMapper extends JpaRepository<PushEventReceipt, Long> {

    @Transactional
    @Modifying
    @Query("delete from PushEventReceipt r where r.createTime < :before")
    int deleteCreatedBefore(@Param("before") LocalDateTime before);
}