package com.grafie.botjava.jx3.ws.service;


import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 接入方需要实现该类
 *
 * @author Grafie
 * @since 1.0.0
 */
@Service
public class WsDataPushService {

    private final GroupInfoMapper groupInfoMapper;

    public WsDataPushService(GroupInfoMapper groupInfoMapper) {
        this.groupInfoMapper = groupInfoMapper;
    }

    /**
     * 推送开启的日期。默认周四。因为有时候周一不维护
     */
    @Value("${my.bot.active-day-of-week:4}")
    private int activeDayOfWeek;

    /**
     * 推送来自ws的数据信息。具体实现类需要又接入方实现
     *
     * @param baseWsData ws数据
     */
    public void pushDataByWs(BaseWsData baseWsData) {
        // 由于官方qq机制的限制，主动推送每个群一个月只允许4条，故只推送开服通知。增加配置项用于推送哪一天的。
        LocalDate localDate = LocalDate.now();
        if (activeDayOfWeek != localDate.getDayOfWeek().getValue()) {
            return;
        }

    }

}


