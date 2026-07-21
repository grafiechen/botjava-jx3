package com.grafie.botjava.jx3.http.data.server;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 搜索区服
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ServerMasterData {
    private String id;
    @JsonAlias("column")
    private String center;
    private String zone;
    private String name;
    private Integer event;
    private JsonNode voice;
    @JsonAlias("abbreviation")
    private List<String> alias;
    @JsonAlias("subordinate")
    private List<String> slave;
}
