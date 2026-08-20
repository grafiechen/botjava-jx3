package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqInteractionResponseDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class QqInteractionClient {

    private static final String INTERACTION_PATH = "/interactions/%s";

    private final QqOpenApiClient openApiClient;

    public QqInteractionClient(QqOpenApiClient openApiClient) {
        this.openApiClient = openApiClient;
    }

    public void respond(String interactionId, QqInteractionResponseDto response) {
        if (response == null) {
            throw new IllegalArgumentException("QQ interaction response must not be null");
        }
        openApiClient.put(String.format(INTERACTION_PATH, safePathSegment(interactionId)),
                ObjectMapperUtil.getObjectMapper().convertValue(response, Map.class), String.class);
    }

    private String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QQ interaction ID must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException("QQ interaction ID must not contain path separator");
        }
        return trimmed;
    }
}