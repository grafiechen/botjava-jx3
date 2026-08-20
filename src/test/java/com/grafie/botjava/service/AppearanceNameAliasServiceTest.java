package com.grafie.botjava.service;

import com.grafie.botjava.entity.AppearanceNameAlias;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.AppearanceNameAliasMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppearanceNameAliasServiceTest {

    private final AppearanceNameAliasMapper aliasMapper = mock(AppearanceNameAliasMapper.class);
    private final AppearanceNamePermissionConfiguration permissionConfiguration = mock(AppearanceNamePermissionConfiguration.class);
    private final AppearanceNameAliasService service = new AppearanceNameAliasService(aliasMapper, permissionConfiguration);

    @Test
    void shouldAddAliasesAsApprovedGloballyWhenReviewIsBypassed() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        when(permissionConfiguration.isAppearanceNameReviewPassed(any(), any())).thenReturn(true);

        AppearanceNameAliasService.AddResult result = service.add(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), "金发·因陀罗", "猴金 后进 猴金1", REGEX.AppearanceNameAliasAdd);

        assertEquals(AppearanceNameAliasService.AddResult.Status.ADDED, result.status());
        assertEquals(List.of("猴金", "后进", "猴金1"), result.aliases());
        ArgumentCaptor<AppearanceNameAlias> captor = ArgumentCaptor.forClass(AppearanceNameAlias.class);
        verify(aliasMapper, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals(AppearanceNameAliasService.GLOBAL_SCOPE, captor.getAllValues().getFirst().getGroupOpenId());
        assertEquals("金发因陀罗", captor.getAllValues().getFirst().getNormalizedCanonicalName());
        assertEquals("猴金", captor.getAllValues().getFirst().getNormalizedAliasName());
        assertEquals(AppearanceNameAliasService.REVIEW_APPROVED, captor.getAllValues().getFirst().getReviewStatus());
        assertEquals("加菲", captor.getAllValues().getFirst().getReviewerName());
        assertTrue(result.reviewPassed());
    }

    @Test
    void shouldKeepPendingPathWhenReviewPolicyIsReenabled() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        when(permissionConfiguration.isAppearanceNameReviewPassed(any(), any())).thenReturn(false);

        AppearanceNameAliasService.AddResult result = service.add(
                message(AppearanceNameAliasService.GLOBAL_SCOPE),
                "金发·因陀罗", "猴金", REGEX.AppearanceNameAliasAdd);

        ArgumentCaptor<AppearanceNameAlias> captor = ArgumentCaptor.forClass(AppearanceNameAlias.class);
        verify(aliasMapper).save(captor.capture());
        assertEquals(AppearanceNameAliasService.REVIEW_PENDING, captor.getValue().getReviewStatus());
        assertFalse(result.reviewPassed());
    }

    @Test
    void shouldRejectAliasMappedToOtherCanonicalNameEvenWhenPending() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        AppearanceNameAlias existing = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·璨月蝶心", "狐金", AppearanceNameAliasService.REVIEW_PENDING);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasName(AppearanceNameAliasService.GLOBAL_SCOPE, "狐金")).thenReturn(existing);

        AppearanceNameAliasService.AddResult result = service.add(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), "金发·因陀罗", "狐金", REGEX.AppearanceNameAliasAdd);

        assertEquals(AppearanceNameAliasService.AddResult.Status.CONFLICT, result.status());
        assertEquals(List.of("狐金->金发·璨月蝶心（待审核）"), result.conflicts());
        verify(aliasMapper, never()).save(any());
    }

    @Test
    void shouldImportSlashSeparatedJx3ApiAliasesAsApproved() {
        AppearanceNameAliasService.TrustedImportResult result = service.importTrustedAliases(
                "金发·因陀罗", "猴金/金发因陀罗");

        assertEquals(List.of("猴金", "金发因陀罗"), result.createdAliases());
        assertTrue(result.approvedAliases().isEmpty());
        assertTrue(result.conflicts().isEmpty());

        ArgumentCaptor<AppearanceNameAlias> captor = ArgumentCaptor.forClass(AppearanceNameAlias.class);
        verify(aliasMapper, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(List.of("猴金", "金发因陀罗"), captor.getAllValues().stream()
                .map(AppearanceNameAlias::getAliasName)
                .toList());
        assertTrue(captor.getAllValues().stream().allMatch(alias ->
                AppearanceNameAliasService.GLOBAL_SCOPE.equals(alias.getGroupOpenId())
                        && AppearanceNameAliasService.REVIEW_APPROVED.equals(alias.getReviewStatus())
                        && "JX3API".equals(alias.getReviewerName())));
    }

    @Test
    void shouldResolveApprovedAliasFromGlobalScopeForAnyGroup() {
        AppearanceNameAlias approved = alias(
                AppearanceNameAliasService.GLOBAL_SCOPE,
                "金发·因陀罗", "猴金", AppearanceNameAliasService.REVIEW_APPROVED);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                AppearanceNameAliasService.GLOBAL_SCOPE, "猴金", AppearanceNameAliasService.REVIEW_APPROVED))
                .thenReturn(approved);
        when(aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗", AppearanceNameAliasService.REVIEW_APPROVED))
                .thenReturn(List.of(approved));

        assertEquals("金发·因陀罗", service.resolve("group-a", "猴金"));
        assertEquals("金发·因陀罗", service.resolve("group-b", "猴金"));
    }

    @Test
    void shouldOnlyLookupApprovedAliases() {
        AppearanceNameAlias first = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·因陀罗", "猴金", AppearanceNameAliasService.REVIEW_APPROVED);
        AppearanceNameAlias second = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·因陀罗", "后进", AppearanceNameAliasService.REVIEW_APPROVED);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                AppearanceNameAliasService.GLOBAL_SCOPE, "猴金", AppearanceNameAliasService.REVIEW_APPROVED)).thenReturn(first);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗", AppearanceNameAliasService.REVIEW_APPROVED)).thenReturn(null);
        when(aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗", AppearanceNameAliasService.REVIEW_APPROVED))
                .thenReturn(List.of(first, second));

        AppearanceNameAliasService.LookupResult byAlias = service.lookup(AppearanceNameAliasService.GLOBAL_SCOPE, "猴金");
        AppearanceNameAliasService.LookupResult byCanonical = service.lookup(AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗");

        assertTrue(byAlias.found());
        assertEquals("金发·因陀罗", byAlias.group().canonicalName());
        assertTrue(byCanonical.found());
        assertEquals(List.of("猴金", "后进"), byCanonical.group().aliases());
    }

    @Test
    void shouldNotResolvePendingName() {
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                AppearanceNameAliasService.GLOBAL_SCOPE, "猴金", AppearanceNameAliasService.REVIEW_APPROVED)).thenReturn(null);
        when(aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameAndReviewStatusOrderByAliasNameAsc(
                AppearanceNameAliasService.GLOBAL_SCOPE, "猴金", AppearanceNameAliasService.REVIEW_APPROVED)).thenReturn(List.of());

        assertEquals("猴金", service.resolve(AppearanceNameAliasService.GLOBAL_SCOPE, "猴金"));
    }

    @Test
    void shouldListPendingAliasesForManagers() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        AppearanceNameAlias pending = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·因陀罗", "猴金", AppearanceNameAliasService.REVIEW_PENDING);
        when(aliasMapper.findByGroupOpenIdAndReviewStatusOrderByNormalizedCanonicalNameAscAliasNameAsc(
                AppearanceNameAliasService.GLOBAL_SCOPE, AppearanceNameAliasService.REVIEW_PENDING)).thenReturn(List.of(pending));

        AppearanceNameAliasService.PendingListResult result = service.pendingList(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), REGEX.AppearanceNameAliasPendingList);

        assertEquals(AppearanceNameAliasService.PendingListResult.Status.ALLOWED, result.status());
        assertEquals("金发·因陀罗", result.groups().getFirst().canonicalName());
        assertEquals(List.of("猴金"), result.groups().getFirst().aliases());
    }

    @Test
    void shouldApprovePendingAlias() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        AppearanceNameAlias pending = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·因陀罗", "猴金", AppearanceNameAliasService.REVIEW_PENDING);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasNameAndReviewStatus(
                AppearanceNameAliasService.GLOBAL_SCOPE, "猴金", AppearanceNameAliasService.REVIEW_PENDING)).thenReturn(pending);

        AppearanceNameAliasService.ReviewResult result = service.approve(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), "猴金", null, REGEX.AppearanceNameAliasApprove);

        assertEquals(AppearanceNameAliasService.ReviewResult.Status.APPROVED, result.status());
        assertEquals(AppearanceNameAliasService.REVIEW_APPROVED, pending.getReviewStatus());
        verify(aliasMapper).save(pending);
    }

    @Test
    void shouldDeleteSingleAliasWhenNameIsAlias() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(true);
        AppearanceNameAlias alias = alias(AppearanceNameAliasService.GLOBAL_SCOPE, "金发·因陀罗", "猴金", AppearanceNameAliasService.REVIEW_PENDING);
        when(aliasMapper.findByGroupOpenIdAndNormalizedAliasName(AppearanceNameAliasService.GLOBAL_SCOPE, "猴金")).thenReturn(alias);
        when(aliasMapper.findByGroupOpenIdAndNormalizedCanonicalNameOrderByAliasNameAsc(AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗"))
                .thenReturn(List.of(alias));
        when(aliasMapper.deleteByGroupOpenIdAndNormalizedCanonicalNameAndNormalizedAliasName(
                AppearanceNameAliasService.GLOBAL_SCOPE, "金发因陀罗", "猴金")).thenReturn(1L);

        AppearanceNameAliasService.DeleteResult result = service.delete(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), "猴金", null, REGEX.AppearanceNameAliasDelete);

        assertEquals(AppearanceNameAliasService.DeleteResult.Status.ALIAS_DELETED, result.status());
        assertEquals("猴金", result.aliasName());
    }

    @Test
    void shouldReturnPermissionDeniedFromPermissionHook() {
        when(permissionConfiguration.canManageAppearanceName(any(), any())).thenReturn(false);

        AppearanceNameAliasService.AddResult result = service.add(
                message(AppearanceNameAliasService.GLOBAL_SCOPE), "金发·因陀罗", "猴金", REGEX.AppearanceNameAliasAdd);

        assertEquals(AppearanceNameAliasService.AddResult.Status.PERMISSION_DENIED, result.status());
        verify(aliasMapper, never()).save(any());
    }

    private AppearanceNameAlias alias(String groupOpenId, String canonicalName, String aliasName, String status) {
        AppearanceNameAlias alias = new AppearanceNameAlias();
        alias.setGroupOpenId(groupOpenId);
        alias.setCanonicalName(canonicalName);
        alias.setNormalizedCanonicalName(service.normalizeName(canonicalName));
        alias.setAliasName(aliasName);
        alias.setNormalizedAliasName(service.normalizeName(aliasName));
        alias.setReviewStatus(status);
        return alias;
    }

    private GroupAtMessageCreateDto message(String groupOpenId) {
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid("member-1");
        author.setUsername("加菲");
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid(groupOpenId);
        message.setAuthor(author);
        return message;
    }
}
