package com.grafie.botjava.jx3.http.data.tieba;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 八卦帖子
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class TiebaRandomData {
    private Long id;
    @JsonAlias("class")
    private String tags;
    private String zone;
    private String server;
    private String name;
    private String title;
    private Long url;
    private String date;
}
