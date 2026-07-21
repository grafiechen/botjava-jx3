package com.grafie.botjava.jx3.http.data.news;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 新闻资讯
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class NewsAllNewsData {
    @JsonProperty("id")
    private Long id;

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
