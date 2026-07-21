package com.grafie.botjava.jx3.http.data.news;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 维护公告
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class NewsAnnounceData {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("catid")
    @JsonAlias("token")
    private String categoryId;

    @JsonProperty("type")
    @JsonAlias("class")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("date")
    private String date;

    @JsonProperty("url")
    private String url;
}
