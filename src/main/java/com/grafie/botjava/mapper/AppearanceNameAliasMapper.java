package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.AppearanceNameAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppearanceNameAliasMapper extends JpaRepository<AppearanceNameAlias, Long> {

    AppearanceNameAlias findByGroupOpenIdAndNormalizedAliasName(String groupOpenId, String normalizedAliasName);

    AppearanceNameAlias findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
            String groupOpenId, String normalizedAliasName, String reviewStatus);

    List<AppearanceNameAlias> findByGroupOpenIdAndNormalizedCanonicalNameOrderByAliasNameAsc(
            String groupOpenId, String normalizedCanonicalName);

    List<AppearanceNameAlias> findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
            String groupOpenId, String normalizedCanonicalName, String reviewStatus);

    List<AppearanceNameAlias> findByGroupOpenIdOrderByNormalizedCanonicalNameAscAliasNameAsc(String groupOpenId);

    List<AppearanceNameAlias> findByGroupOpenIdAndReviewStatusOrderByNormalizedCanonicalNameAscAliasNameAsc(
            String groupOpenId, String reviewStatus);

    long deleteByGroupOpenIdAndNormalizedCanonicalName(String groupOpenId, String normalizedCanonicalName);

    long deleteByGroupOpenIdAndNormalizedCanonicalNameAndNormalizedAliasName(
            String groupOpenId, String normalizedCanonicalName, String normalizedAliasName);
}
