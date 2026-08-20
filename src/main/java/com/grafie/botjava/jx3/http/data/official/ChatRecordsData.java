package com.grafie.botjava.jx3.http.data.official;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRecordsData {
    private Integer total;
    private List<ChatRecord> list = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatRecord {
        private String zone;
        private String server;
        private String roleName;
        private String roleId;
        private String globalId;
        private String channel;
        private String message;
        private Long time;
    }
}
