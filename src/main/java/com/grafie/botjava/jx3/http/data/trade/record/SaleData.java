package com.grafie.botjava.jx3.http.data.trade.record;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaleData {
    @JsonProperty("id")
    private String id;

    @JsonProperty("index")
    private String index;

    @JsonProperty("zone")
    private String zone;

    @JsonProperty("server")
    private String server;

    @JsonProperty("value")
    private Integer value;

    @JsonAlias("sales")
    @JsonProperty("sale")
    private Integer sales;

    @JsonProperty("token")
    private String token;

    @JsonProperty("source")
    private Integer source;

    @JsonProperty("date")
    private String date;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("datetime")
    private String datetime;

}
