package com.grafie.botjava.jx3.ws.data.frontline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.WsActionData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 前线战况-据点占领（有帮会）
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 1104)
public class WsDataAction1104 extends BaseWsData {
    @JsonProperty("camp_name")
    private String campName;

    @JsonProperty("castle")
    private String castle;

    @JsonProperty("tong_name")
    private String tongName;

}
