package com.grafie.botjava.jx3.ws.data.frontline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.WsActionData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 前线战况-大旗被夺
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 1103)
public class WsDataAction1103 extends BaseWsData {
    @JsonProperty("camp_name")
    private String campName;

    @JsonProperty("map_name")
    private String mapName;

    @JsonProperty("castle")
    private String castle;
}
