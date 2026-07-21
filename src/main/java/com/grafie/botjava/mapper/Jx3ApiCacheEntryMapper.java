package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.Jx3ApiCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface Jx3ApiCacheEntryMapper extends JpaRepository<Jx3ApiCacheEntry, Long> {

    Jx3ApiCacheEntry findByCacheKey(String cacheKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update Jx3ApiCacheEntry c
               set c.requestPath = :requestPath,
                   c.responseJson = :responseJson,
                   c.expiresAt = :expiresAt,
                   c.updateTime = :now
             where c.cacheKey = :cacheKey
            """)
    int updateEntry(@Param("cacheKey") String cacheKey,
                    @Param("requestPath") String requestPath,
                    @Param("responseJson") String responseJson,
                    @Param("expiresAt") Instant expiresAt,
                    @Param("now") Instant now);

    long deleteByExpiresAtLessThanEqual(Instant threshold);
}
