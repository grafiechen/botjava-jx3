package com.grafie.botjava.jx3.ws.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 诛恶事件
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 1009)
public class WsDataAction1009 extends BaseWsData{
    @JsonProperty("map_name")
    private String mapName;
}
