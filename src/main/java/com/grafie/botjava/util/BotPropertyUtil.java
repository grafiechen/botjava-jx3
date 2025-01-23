package com.grafie.botjava.util;

import com.grafie.botjava.config.TxBotProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:00
 */
@Component
public class BotPropertyUtil {
    private final TxBotProperty initTxBotProperty;
    private static TxBotProperty txBotProperty;

    public BotPropertyUtil(TxBotProperty initTxBotProperty) {
        this.initTxBotProperty = initTxBotProperty;
    }


    @PostConstruct
    public void init() {
        txBotProperty = initTxBotProperty;
    }

    /**
     * 获取bot secret
     *
     * @return String
     */
    public static String getBotSecret() {
        return txBotProperty.getClientSecret();
    }
}
