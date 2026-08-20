package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqUrlLinkRequestDto;
import com.grafie.botjava.entity.dto.qq.QqUrlLinkResultDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class QqUrlLinkClient {

    private static final String URL_LINK_PATH = "/v2/generate_url_link";

    private final QqOpenApiClient openApiClient;

    public QqUrlLinkClient(QqOpenApiClient openApiClient) {
        this.openApiClient = openApiClient;
    }

    public QqUrlLinkResultDto generate(QqUrlLinkRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("QQ URL link request must not be null");
        }
        return openApiClient.post(URL_LINK_PATH,
                ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class),
                QqUrlLinkResultDto.class);
    }
}