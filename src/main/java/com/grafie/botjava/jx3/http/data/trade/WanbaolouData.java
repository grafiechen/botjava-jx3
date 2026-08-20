package com.grafie.botjava.jx3.http.data.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * JX3API 万宝楼编号搜索响应。所有字段均为可选，兼容上游按账号返回不同字段集合。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WanbaolouData {
    private String id;
    private String serverName;
    private String roleName;
    private Integer roleLevel;
    private Integer followNum;
    private String forceName;
    private String bodyName;
    private Integer meetingNum;
    private String campName;
    private String equipScore;
    private Integer seniorityNum;
    private Integer priceNum;
    private Integer tradeStatus;
    private List<UpdatePrice> updatePrices = new ArrayList<>();
    private String replyTitle;
    private String replyContent;
    private Long replyTime;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdatePrice {
        private String accoSeq;
        private Long createtime;
        private Integer updatePrice;
        private Long updateTime;
        private String zhanghaoId;
    }
}