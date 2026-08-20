package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqInteractionResponseDto {

    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int FREQUENT = 2;
    public static final int DUPLICATED = 3;
    public static final int FORBIDDEN = 4;
    public static final int ADMIN_ONLY = 5;

    private Integer code;

    public static QqInteractionResponseDto of(int code) {
        QqInteractionResponseDto response = new QqInteractionResponseDto();
        response.setCode(code);
        return response;
    }
}