package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.BotAdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BotAdminAuditLogMapper extends JpaRepository<BotAdminAuditLog, Long> {

    List<BotAdminAuditLog> findTop10ByOrderByCreateTimeDesc();
}