package com.grafie.botjava.service;

import com.grafie.botjava.entity.PushEventReceipt;
import com.grafie.botjava.mapper.PushEventReceiptMapper;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushEventDeduplicationServiceTest {

    private static final String FINGERPRINT = "a".repeat(64);

    @Mock
    private PushEventReceiptMapper mapper;

    private PushTaskDefinition task;

    @BeforeEach
    void setUp() {
        task = new PushTaskRegistry().find("开服状态").orElseThrow();
    }

    @Test
    void firstReceiptShouldClaimEvent() {
        when(mapper.saveAndFlush(any(PushEventReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PushEventDeduplicationService service = new PushEventDeduplicationService(mapper, 7);

        assertThat(service.tryClaim(task, FINGERPRINT)).isTrue();
    }

    @Test
    void duplicateUniqueKeyShouldSkipEvent() {
        when(mapper.saveAndFlush(any(PushEventReceipt.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        PushEventDeduplicationService service = new PushEventDeduplicationService(mapper, 7);

        assertThat(service.tryClaim(task, FINGERPRINT)).isFalse();
    }
}