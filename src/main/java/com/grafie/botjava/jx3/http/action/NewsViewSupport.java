package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.http.data.news.NewsAllNewsData;
import com.grafie.botjava.jx3.http.data.news.NewsAnnounceData;

import java.util.ArrayList;
import java.util.List;

final class NewsViewSupport {

    private NewsViewSupport() {
    }

    static List<NewsItemView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<NewsItemView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof NewsAllNewsData news) {
                views.add(new NewsItemView(news.getType(), news.getTitle(), news.getDate()));
            } else if (item instanceof NewsAnnounceData news) {
                views.add(new NewsItemView(news.getType(), news.getTitle(), news.getDate()));
            }
        }
        return List.copyOf(views);
    }
}
