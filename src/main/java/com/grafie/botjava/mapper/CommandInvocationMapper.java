package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.CommandInvocation;
import com.grafie.botjava.entity.CommandInvocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandInvocationMapper extends JpaRepository<CommandInvocation, Long> {

    long deleteByCreateTimeBefore(LocalDateTime threshold);

    @Query("""
            select c.commandName as commandName,
                   count(c) as invocationCount,
                   sum(case when c.status = :successStatus then 1 else 0 end) as successCount,
                   avg(c.elapsedMillis) as averageElapsedMillis
            from CommandInvocation c
            where c.groupOpenId = :groupOpenId and c.createTime >= :since
            group by c.commandName
            order by count(c) desc, c.commandName asc
            """)
    List<CommandInvocationSummary> summarizeByGroup(
            @Param("groupOpenId") String groupOpenId,
            @Param("since") LocalDateTime since,
            @Param("successStatus") CommandInvocationStatus successStatus
    );
}
