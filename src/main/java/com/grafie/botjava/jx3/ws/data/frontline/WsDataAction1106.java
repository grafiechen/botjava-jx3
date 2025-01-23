package com.grafie.botjava.jx3.ws.data.frontline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.WsActionData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 前线战况-结算贡献
 *
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 1106)
public class WsDataAction1106 extends BaseWsData {

    @JsonProperty("camp_name")
    private String campName;

    @JsonProperty("tong_list")
    private List<String> tongList;
}
