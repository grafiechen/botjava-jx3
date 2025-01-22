package com.grafie.botjava.jx3.http.data.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 查看状态
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ServerStatusData {
    @JsonProperty("zone")
    private String zone;

    @JsonProperty("server")
    private String server;

    @JsonProperty("status")
    private String status;
}
