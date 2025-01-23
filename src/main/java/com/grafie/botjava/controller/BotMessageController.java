package com.grafie.botjava.controller;

import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.service.BotMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author grafie.chen
 * @since 2025/1/22  10:50
 */
@Slf4j
@RequestMapping("/bot/message")
@RestController
public class BotMessageController {
    @Resource
    private BotMessageService botMessageService;

    @PostMapping()
    public Object getMessage(@RequestBody Payload payload) {
        try {
            log.info("接收到推送消息 =>{}", payload);
            return botMessageService.dealMessage(payload);
        } catch (Exception e) {
            log.error("处理消息出错请求参数=>{}", payload, e);
            return null;
        }

    }

}
