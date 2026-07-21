package com.grafie.botjava.jx3.http.action;

public record RoleCardView(String zone, String server, String roleName, Integer showIndex,
                           Boolean active, String cacheTime, String imageDataUri) {
}
