package com.grafie.botjava.service.push;

/**
 * 可订阅推送任务的稳定定义。code 用于持久化，displayName 用于 QQ 指令。
 */
public record PushTaskDefinition(String code,
                                 String displayName,
                                 String category,
                                 PushTaskSource source,
                                 Integer wsActionCode,
                                 boolean producerReady) {
}
