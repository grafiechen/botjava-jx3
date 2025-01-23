package com.grafie.botjava.jx3.ws.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.grafie.botjava.util.TimeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 关隘预告
 *
 * @author Grafie
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@WsActionData(actionCode = 2005)
public class WsDataAction2005 extends BaseWsData {
    @JsonProperty("castle")
    private String castle;

    private String start;

    public void setStart(long start) {
        this.start = TimeUtils.timeFormatting(start);
    }
}
