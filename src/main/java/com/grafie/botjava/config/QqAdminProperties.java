package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bot.qq.admin")
public class QqAdminProperties {

    private List<String> masterOpenids = new ArrayList<>();

    public List<String> getMasterOpenids() {
        return masterOpenids;
    }

    public void setMasterOpenids(List<String> masterOpenids) {
        this.masterOpenids = masterOpenids == null ? new ArrayList<>() : masterOpenids;
    }

    public boolean isMasterOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            return false;
        }
        String normalized = openid.trim();
        return masterOpenids.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .anyMatch(normalized::equals);
    }
}