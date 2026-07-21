package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.http.data.role.RoleShowCardData;
import com.grafie.botjava.jx3.http.data.role.RoleShowRandomData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class RoleCardTemplateSupport {

    private static final int MAX_CARDS = 12;

    private RoleCardTemplateSupport() {
    }

    static List<RoleCardView> toViews(Object value, Jx3RequestUtil requestUtil) {
        List<RoleCardView> views = new ArrayList<>();
        Map<String, Optional<String>> images = new HashMap<>();
        if (value instanceof List<?> values) {
            for (Object item : values) {
                addView(views, item, requestUtil, images);
                if (views.size() >= MAX_CARDS) {
                    break;
                }
            }
        } else {
            addView(views, value, requestUtil, images);
        }
        return List.copyOf(views);
    }

    private static void addView(List<RoleCardView> views, Object value, Jx3RequestUtil requestUtil,
                                Map<String, Optional<String>> images) {
        if (value instanceof RoleShowCardData card) {
            views.add(new RoleCardView(card.getZone(), card.getServer(), card.getName(), card.getShowIndex(),
                    card.getShowActive(), card.getCache(), image(requestUtil, card.getStaticUrl(), images)));
        } else if (value instanceof RoleShowRandomData card) {
            views.add(new RoleCardView(card.getZone(), card.getServer(), card.getName(), card.getShowIndex(),
                    null, null, image(requestUtil, card.getAvatar(), images)));
        }
    }

    private static String image(Jx3RequestUtil requestUtil, String url,
                                Map<String, Optional<String>> images) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return images.computeIfAbsent(url, requestUtil::loadRemoteImageDataUri).orElse(null);
    }
}
