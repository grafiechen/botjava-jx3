package com.person.botjava.util;

/**
 * 用于计算Intents的工具类
 *
 * @author grafie.chen
 * @since 2024/7/16  17:17
 */
public class IntentsUtil {
    /**
     * 是否打开频道事件的监听
     * - GUILD_CREATE           // 当机器人加入新guild时
     * - GUILD_UPDATE           // 当guild资料发生变更时
     * - GUILD_DELETE           // 当机器人退出guild时
     * - CHANNEL_CREATE         // 当channel被创建时
     * - CHANNEL_UPDATE         // 当channel被更新时
     * - CHANNEL_DELETE         // 当channel被删除时
     */
    private static final int GUIDS = 1;
    /**
     * 是否打开频道成员事件的监听
     * - GUILD_MEMBER_ADD       // 当成员加入时
     * - GUILD_MEMBER_UPDATE    // 当成员资料变更时
     * - GUILD_MEMBER_REMOVE    // 当成员被移除时
     */
    private static final int GUILD_MEMBER = 1 << 1;
    /**
     * 是否打开消息事件的监听          // 消息事件，仅 *私域* 机器人能够设置此 intents。
     * - MESSAGE_CREATE         // 发送消息事件，代表频道内的全部消息，而不只是 at 机器人的消息。内容与 AT_MESSAGE_CREATE 相同
     * - MESSAGE_DELETE         // 删除（撤回）消息事件
     */
    private static final int GUILD_MESSAGE = 1 << 9;
    /**
     * 是否打开消息相关互动事件的监听
     * - MESSAGE_REACTION_ADD    // 为消息添加表情表态
     * - MESSAGE_REACTION_REMOVE // 为消息删除表情表态
     */
    private static final int GUILD_MESSAGE_REACTIONS = 1 << 10;
    /**
     * 是否打开私信事件的监听
     * - DIRECT_MESSAGE_CREATE   // 当收到用户发给机器人的私信消息时
     * - DIRECT_MESSAGE_DELETE   // 删除（撤回）消息事件
     */
    private static final int DIRECT_MESSAGE = 1 << 12;
    /**
     * 是否打开互动事件的监听
     * - INTERACTION_CREATE     // 互动事件创建时
     */
    private static final int INTERACTION = 1 << 26;
    /**
     * 是否打开消息审核事件的监听
     * - MESSAGE_AUDIT_PASS     // 消息审核通过
     * - MESSAGE_AUDIT_REJECT   // 消息审核不通过
     */
    private static final int MESSAGE_AUDIT = 1 << 27;
    /**
     * 是否打开论坛事件的监听// 论坛事件，仅 *私域* 机器人能够设置此 intents。
     * - FORUM_THREAD_CREATE     // 当用户创建主题时
     * - FORUM_THREAD_UPDATE     // 当用户更新主题时
     * - FORUM_THREAD_DELETE     // 当用户删除主题时
     * - FORUM_POST_CREATE       // 当用户创建帖子时
     * - FORUM_POST_DELETE       // 当用户删除帖子时
     * - FORUM_REPLY_CREATE      // 当用户回复评论时
     * - FORUM_REPLY_DELETE      // 当用户删除评论时
     * - FORUM_PUBLISH_AUDIT_RESULT      // 当用户发表审核通过时
     */
    private static final int OPEN_FORUMS_EVENT = 1 << 28;
    /**
     * 是否打开音频事件的监听
     * - AUDIO_START             // 音频开始播放时
     * - AUDIO_FINISH            // 音频播放结束时
     * - AUDIO_ON_MIC            // 上麦时
     * - AUDIO_OFF_MIC           // 下麦时
     */
    private static final int AUDIO_ACTION = 1 << 29;
    /**
     * 是否打开公域消息事件的监听 // 消息事件，此为公域的消息事件
     * - AT_MESSAGE_CREATE       // 当收到@机器人的消息时
     * - PUBLIC_MESSAGE_DELETE   // 当频道的消息被删除时
     */
    private static final int PUBLIC_GUILD_MESSAGES = 1 << 30;
}
