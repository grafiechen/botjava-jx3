package com.grafie.botjava.jx3.ws.data.dilu;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.WsActionData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 的卢 刷新
 *
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 1011)
public class WsDataAction1011 extends BaseWsData {

    @JsonProperty("map_name")
    private String mapName;

    @JsonProperty("name")
    private String name;

}
