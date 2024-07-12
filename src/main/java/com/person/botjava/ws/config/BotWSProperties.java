package com.person.botjava.ws.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 机器人ws配置参数
 *
 * @author grafie.chen
 * @since 2024/7/12  16:57
 */
@Configuration
@ConfigurationProperties(prefix = "my.bot.ws")
@Data
public class BotWSProperties {
    /**
     * 是创建机器人的时候分配的，格式为Bot {appid}.{app_token}
     */
    private String token;
    /**
     * shard 该参数是用来进行水平分片的。该参数是个拥有两个元素的数组。例如：[0,4]，代表分为四个片，当前链接是第 0 个片，业务稍后应该继续建立 shard 为[1,4],[2,4],[3,4]的链接，才能完整接收事件。更多详细的内容可以参考Shard。
     * 需要填写成在正确的 [0,1] 这种格式
     */
    private String shard;
    /**
     * properties 目前无实际作用，可以按照自己的实际情况填写，也可以留空,
     * 需要根据实际情况填写成正确的 jsonString
     */
    private String properties;
    /**
     * intents 是此次连接所需要接收的事件，具体可参考  <a href="https://bot.q.qq.com/wiki/develop/api/gateway/intents.html">Intents</a>
     */
    private Integer intents;
}
