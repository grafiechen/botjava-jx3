package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.util.BotRequestUtl;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:49
 */
@Slf4j
@BotMessageAction
@SuppressWarnings("Unchecked")
public class GroupAtMessageAction extends BaseAction {


    private final BotRequestUtl botRequestUtl;
    /**
     * 群消息发送地址
     */
    private String GROUP_MESSAGE = "/v2/groups/%s/messages";

    public GroupAtMessageAction(BotRequestUtl botRequestUtl) {
        this.botRequestUtl = botRequestUtl;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        // 根据正则表达式，提取出来指令和内容
        GroupAtMessageCreateDto atMessageDto = ObjectMapperUtil.readValue(payload.getD(), GroupAtMessageCreateDto.class);
        if (atMessageDto.getContent() == null) {
            log.info("群消息内容为空，payload=>{}", payload);
            return null;
        }
        String requestRegex = atMessageDto.getContent().replace("/", "").trim();
        TxMessageInfo baseResult = buildApprovalMessage(requestRegex);
        if (baseResult == null) {
            REGEX regex = REGEX.matchEnum(requestRegex);
            if (regex == null) {
                log.info("未匹配到可处理的群消息命令，payload=>{}", payload);
                return null;
            }
            Jx3BaseAction jx3BaseAction
                    = (Jx3BaseAction) SpringContextUtil.getBean(regex.getBaseAction());
            baseResult = jx3BaseAction.doRequest(atMessageDto, requestRegex, regex);
        }
        baseResult.setMsg_id(atMessageDto.getId());
        // group_at_message 没有需要返回的数据
        Map<String, Object> base = ObjectMapperUtil.getObjectMapper().convertValue(baseResult, Map.class);
        log.info("回复群消息，http调用结果=>{}", botRequestUtl.doPost(String.format(GROUP_MESSAGE, atMessageDto.getGroupOpenid()), base, String.class));
        // 这里不需要回复任何消息
        return null;
    }


    /*
    * 以下内容为临时测试使用，完成测试后，将删除，并使用正常的调用。
    *
    * */


    private TxMessageInfo buildApprovalMessage(String content) {
        if ("test".equalsIgnoreCase(content)) {
            return buildTextMessage("测试消息已收到");
        }
        if ("签到".equalsIgnoreCase(content)) {
            return dailySign();
        }
        if ("打坐".equalsIgnoreCase(content)) {
            return daZuo();
        }
        TxMessageInfo docMessage = buildJx3ApiDocApprovalMessage(content);
        if (docMessage != null) {
            return docMessage;
        }
        REGEX regex = REGEX.matchEnum(content);
        if (regex == null) {
            return null;
        }
        Map<String, String> params = regex.handleEncounter(content);
        return switch (regex) {
            case BindServerCalendar -> buildBindServerCalendarApprovalMessage(params);
            case ActiveCurrent -> buildActiveCurrentApprovalMessage(params);
            case ServerCheck -> buildServerCheckApprovalMessage(params);
            case TradeDemon -> buildTradeDemonApprovalMessage(params);
            case TradeRecord -> buildTradeRecordApprovalMessage(params);
            case SaohuaRandom -> buildSaohuaRandomApprovalMessage(params);
            case NewsAnnounce -> buildNewsAnnounceApprovalMessage(params);
            case LuckAdventure -> buildLuckAdventureApprovalMessage(params);
            case LuckStatistical -> buildLuckStatisticalApprovalMessage(params);
            case MatchRecent -> buildMatchRecentApprovalMessage(params);
            case RoleAttribute -> buildRoleAttributeApprovalMessage(params);
            case WatchRecord -> buildWatchRecordApprovalMessage(params);
            case MemberRecruit -> buildMemberRecruitApprovalMessage(params);
            case ServerSand -> buildServerSandApprovalMessage(params);
            case ActiveMonster -> buildActiveMonsterApprovalMessage(params);
            case SchoolForce -> buildSchoolForceApprovalMessage(params);
            case DungeonRecord -> buildDungeonRecordApprovalMessage(params);
            case LuckAdventurePs -> buildLuckAdventurePsApprovalMessage(params);
            case DpsCompute -> buildDpsComputeApprovalMessage(params);
            case ChiGua -> buildChiGuaApprovalMessage(params);
        };
    }

    /**
     * 签到
     */
    private TxMessageInfo dailySign() {
        return buildTextMessage("签到成功，今日奖励已记录。");
    }

    /**
     * 打坐
     */
    private TxMessageInfo daZuo() {
        return buildTextMessage("打坐成功，气力值恢复中。");
    }

    private TxMessageInfo buildJx3ApiDocApprovalMessage(String content) {
        String command = content.trim().split("\\s+", 2)[0];
        return switch (command) {
            case "活动日历" -> buildActiveCalendarDocApprovalMessage();
            case "活动月历" -> buildActiveListCalendarDocApprovalMessage();
            case "行侠事件" -> buildActiveCelebsDocApprovalMessage();
            case "科举答题" -> buildExamSearchDocApprovalMessage();
            case "家园鲜花" -> buildHomeFlowerDocApprovalMessage();
            case "家园装饰" -> buildHomeFurnitureDocApprovalMessage();
            case "器物图谱" -> buildHomeTravelDocApprovalMessage();
            case "新闻资讯" -> buildNewsAllNewsDocApprovalMessage();
            case "维护公告" -> buildNewsAnnounceDocApprovalMessage();
            case "搜索区服" -> buildMasterSearchDocApprovalMessage();
            case "开服状态" -> buildStatusCheckDocApprovalMessage();
            case "技改记录" -> buildSkillReworkDocApprovalMessage();
            case "小药推荐" -> buildSchoolFoodsDocApprovalMessage();
            case "百战首领" -> buildActiveMonsterDocApprovalMessage();
            case "扶摇预测" -> buildActiveNextEventDocApprovalMessage();
            case "阵营拍卖" -> buildAuctionRecordsDocApprovalMessage();
            case "的卢记录" -> buildSteedRecordsDocApprovalMessage();
            case "烟花记录" -> buildShowRecordsDocApprovalMessage();
            case "骗子查询" -> buildFraudDetailDocApprovalMessage();
            case "角色奇遇" -> buildEventRecordsDocApprovalMessage();
            case "未做奇遇" -> buildEventUnfinishedDocApprovalMessage();
            case "近期奇遇" -> buildEventRecentDocApprovalMessage();
            case "奇遇统计" -> buildEventStatisticsDocApprovalMessage();
            case "奇遇汇总" -> buildEventCollectDocApprovalMessage();
            case "名剑战绩" -> buildArenaRecentDocApprovalMessage();
            case "名剑排行" -> buildArenaAwesomeDocApprovalMessage();
            case "名剑统计" -> buildArenaSchoolsDocApprovalMessage();
            case "团队招募" -> buildRecruitSearchDocApprovalMessage();
            case "师徒系统" -> buildMentorSearchDocApprovalMessage();
            case "本服榜单" -> buildRankStatisticalDocApprovalMessage();
            case "掉落统计" -> buildRewardStatisticsDocApprovalMessage();
            case "角色信息" -> buildRoleDetailDocApprovalMessage();
            case "角色名片" -> buildCardRecordDocApprovalMessage();
            case "所有名片" -> buildCardRecordsDocApprovalMessage();
            case "随机名片" -> buildCardRandomDocApprovalMessage();
            case "缓存名片" -> buildCardCachedDocApprovalMessage();
            case "角色百战" -> buildRoleMonsterDocApprovalMessage();
            case "心法阵眼" -> buildSchoolMatrixDocApprovalMessage();
            case "奇穴详情" -> buildSchoolTalentDocApprovalMessage();
            case "技能详情" -> buildSchoolSkillsDocApprovalMessage();
            case "资历排行" -> buildSchoolSeniorityDocApprovalMessage();
            case "阵营沙盘" -> buildSandRecordsDocApprovalMessage();
            case "阵营事件" -> buildFenxianRecordsDocApprovalMessage();
            case "诛恶事件" -> buildSmiteRecordsDocApprovalMessage();
            case "关隘首领" -> buildMineCartDocApprovalMessage();
            case "本日赤兔" -> buildChituRecordsDocApprovalMessage();
            case "本周赤兔" -> buildChituWeekRecordsDocApprovalMessage();
            case "马场刷新" -> buildRanchRecordsDocApprovalMessage();
            case "试炼排行" -> buildRankTrialsDocApprovalMessage();
            case "贴吧物价" -> buildTiebaItemRecordsDocApprovalMessage();
            case "金币价格" -> buildTradeDemonDocApprovalMessage();
            case "黑市物价" -> buildTradeRecordsDocApprovalMessage();
            case "搜索物品" -> buildTradeItemSearchDocApprovalMessage();
            case "物品价格" -> buildTradeItemRecordsDocApprovalMessage();
            case "帮战记录" -> buildBattleRecordsDocApprovalMessage();
            case "副本解密" -> buildMechCalculatorDocApprovalMessage();
            case "统战歪歪" -> buildDuowanStatisticsDocApprovalMessage();
            case "八卦帖子" -> buildTiebaRandomDocApprovalMessage();
            case "世界骚话" -> buildSaohuaRandomDocApprovalMessage();
            case "舔狗日记" -> buildSaohuaContentDocApprovalMessage();
            default -> null;
        };
    }


    private TxMessageInfo buildActiveCalendarDocApprovalMessage() {
        return buildTextMessage("2025-12-03 周三\n大战：英雄冰川宫宝库\n战场：浮香丘\n宗门：明教·漫漫朝圣路");
    }

    private TxMessageInfo buildActiveListCalendarDocApprovalMessage() {
        return buildTextMessage("2025-11-17 周一\n大战：英雄不染窟\n战场：雪域关城\n宗门：唐门·兄弟之争");
    }

    private TxMessageInfo buildActiveCelebsDocApprovalMessage() {
        return buildTextMessage("行侠事件：晟江·恶霸出浴\n地点：白菰里\n时间：12:56");
    }

    private TxMessageInfo buildExamSearchDocApprovalMessage() {
        return buildTextMessage("科举题：古琴有几根弦？\n答案：七根");
    }

    private TxMessageInfo buildHomeFlowerDocApprovalMessage() {
        return buildTextMessage("九寨沟·镜海：一级绣球花\n颜色：红、白、紫\n参考价：1.5");
    }

    private TxMessageInfo buildHomeFurnitureDocApprovalMessage() {
        return buildTextMessage("家园装饰：龙门香梦\n来源：大水南方令\n描述：跳影如流泉，香梦过龙门。");
    }

    private TxMessageInfo buildHomeTravelDocApprovalMessage() {
        return buildTextMessage("器物图谱：日晷\n来源：宠物游历\n产出地图：万花");
    }

    private TxMessageInfo buildNewsAllNewsDocApprovalMessage() {
        return buildTextMessage("官方新闻：全新特惠发型亮相\n日期：12/01");
    }

    private TxMessageInfo buildNewsAnnounceDocApprovalMessage() {
        return buildTextMessage("官方公告：11月24日1.0.0.9486版本更新公告\n日期：11/24");
    }

    private TxMessageInfo buildMasterSearchDocApprovalMessage() {
        return buildTextMessage("区服：电信区·梦江南\n别名：双梦镇、双梦\n合服：梦江南、枫泾古镇、如梦令");
    }

    private TxMessageInfo buildStatusCheckDocApprovalMessage() {
        return buildTextMessage("电信区·长安城\n开服状态：爆满");
    }

    private TxMessageInfo buildSkillReworkDocApprovalMessage() {
        return buildTextMessage("技改记录：山海源流资料片武学调整\n时间：2025-11-17 08:52:08");
    }

    private TxMessageInfo buildSchoolFoodsDocApprovalMessage() {
        return buildTextMessage("万花·花间游\n推荐：风语·灌汤包\n类型：增强食品，内功");
    }

    private TxMessageInfo buildActiveMonsterDocApprovalMessage() {
        return buildTextMessage("百战首领：拓跋思南\n示例首领：秦雷\n技能：积气法门、霞月长针");
    }

    private TxMessageInfo buildActiveNextEventDocApprovalMessage() {
        return buildTextMessage("扶摇预测：电信区·长安城\n状态：即将刷新");
    }

    private TxMessageInfo buildAuctionRecordsDocApprovalMessage() {
        return buildTextMessage("阵营拍卖：唯我独尊\n副本：25人普通会战弓月城\n物品：昆玉玄晶");
    }

    private TxMessageInfo buildSteedRecordsDocApprovalMessage() {
        return buildTextMessage("的卢记录：长安城·龙泉府\n捕获：慕深深\n成交：805万1514金");
    }

    private TxMessageInfo buildShowRecordsDocApprovalMessage() {
        return buildTextMessage("烟花记录：成都\n夜温言 送给 紫苏苏\n烟花：真橙之心");
    }

    private TxMessageInfo buildFraudDetailDocApprovalMessage() {
        return buildTextMessage("骗子查询：剑网3\n标题：避雷骗子代清\n摘要：避雷QQ相关记录");
    }

    private TxMessageInfo buildEventRecordsDocApprovalMessage() {
        return buildTextMessage("角色奇遇：梦江南·狸嫁\n奇遇：泛天河");
    }

    private TxMessageInfo buildEventUnfinishedDocApprovalMessage() {
        return buildTextMessage("未做奇遇：三山四海\n类型：绝世奇遇");
    }

    private TxMessageInfo buildEventRecentDocApprovalMessage() {
        return buildTextMessage("近期奇遇：梦江南·往矣\n奇遇：侠者成歌");
    }

    private TxMessageInfo buildEventStatisticsDocApprovalMessage() {
        return buildTextMessage("奇遇统计：长安城·啵啵鱼\n奇遇：阴阳两界");
    }

    private TxMessageInfo buildEventCollectDocApprovalMessage() {
        return buildTextMessage("奇遇汇总：长安城\n奇遇：三山四海\n次数：9");
    }

    private TxMessageInfo buildArenaRecentDocApprovalMessage() {
        return buildTextMessage("名剑战绩：梦江南·世界第一路人\n门派：藏剑\n3v3：2268，胜率51%");
    }

    private TxMessageInfo buildArenaAwesomeDocApprovalMessage() {
        return buildTextMessage("名剑排行：梦江南·长风清晏\n门派：纯阳\n排名：1，分数：2973");
    }

    private TxMessageInfo buildArenaSchoolsDocApprovalMessage() {
        return buildTextMessage("名剑统计：万花\n本期：0\n上期：66");
    }

    private TxMessageInfo buildRecruitSearchDocApprovalMessage() {
        return buildTextMessage("团队招募：唯我独尊\n活动：25人英雄会战弓月城\n团长：老实四");
    }

    private TxMessageInfo buildMentorSearchDocApprovalMessage() {
        return buildTextMessage("师徒系统：长安城·不见风澜\n门派：刀宗\n需求：找个奶妈师傅");
    }

    private TxMessageInfo buildRankStatisticalDocApprovalMessage() {
        return buildTextMessage("本服榜单：长安城\n榜单：赛季浩气五十强\n帮会：追梦烟雨");
    }

    private TxMessageInfo buildRewardStatisticsDocApprovalMessage() {
        return buildTextMessage("掉落统计：长安城\n副本：25人普通会战弓月城\n物品：昆玉玄晶");
    }

    private TxMessageInfo buildRoleDetailDocApprovalMessage() {
        return buildTextMessage("角色信息：唯我独尊·夜温言@长安城\n门派：万花\n阵营：中立");
    }

    private TxMessageInfo buildCardRecordDocApprovalMessage() {
        return buildTextMessage("角色名片：唯我独尊·夜温言@长安城\n名片序号：2");
    }

    private TxMessageInfo buildCardRecordsDocApprovalMessage() {
        return buildTextMessage("所有名片：唯我独尊·夜温言@长安城\n当前启用名片：2");
    }

    private TxMessageInfo buildCardRandomDocApprovalMessage() {
        return buildTextMessage("随机名片：唯我独尊·山色暮相依\n名片序号：1");
    }

    private TxMessageInfo buildCardCachedDocApprovalMessage() {
        return buildTextMessage("缓存名片：唯我独尊·夜温言@长安城\n缓存已命中");
    }

    private TxMessageInfo buildRoleMonsterDocApprovalMessage() {
        return buildTextMessage("角色百战：唯我独尊·夜温言@长安城\n技能数：131\n示例技能：空穴来风");
    }

    private TxMessageInfo buildSchoolMatrixDocApprovalMessage() {
        return buildTextMessage("心法阵眼：花间游\n阵法：七绝逍遥阵\n效果：阅历、声望、内功攻击提高");
    }

    private TxMessageInfo buildSchoolTalentDocApprovalMessage() {
        return buildTextMessage("奇穴详情：芳姿畅音\n门派：七秀\n调息：18秒");
    }

    private TxMessageInfo buildSchoolSkillsDocApprovalMessage() {
        return buildTextMessage("技能详情：锋针\n套路：太素九针\n距离：20尺");
    }

    private TxMessageInfo buildSchoolSeniorityDocApprovalMessage() {
        return buildTextMessage("资历排行：长安城·卿兮\n门派：万花\n资历：184995");
    }

    private TxMessageInfo buildSandRecordsDocApprovalMessage() {
        return buildTextMessage("阵营沙盘：长安城\n据点：金门关\n帮会：追梦烟雨");
    }

    private TxMessageInfo buildFenxianRecordsDocApprovalMessage() {
        return buildTextMessage("阵营事件：恶人谷\n分线：梦江南\n角色：欧薏米·天鹅坪");
    }

    private TxMessageInfo buildSmiteRecordsDocApprovalMessage() {
        return buildTextMessage("诛恶事件：唯我独尊\n地图：银霜口");
    }

    private TxMessageInfo buildMineCartDocApprovalMessage() {
        return buildTextMessage("关隘首领：剑胆琴心\n关隘：赤焰关\n状态：保护期");
    }

    private TxMessageInfo buildChituRecordsDocApprovalMessage() {
        return buildTextMessage("本日赤兔：破阵子\n地图：黑戈壁\n日期：2025-12-01");
    }

    private TxMessageInfo buildChituWeekRecordsDocApprovalMessage() {
        return buildTextMessage("本周赤兔：破阵子\n地图：黑戈壁\n日期：2025-12-01");
    }

    private TxMessageInfo buildRanchRecordsDocApprovalMessage() {
        return buildTextMessage("马场刷新：梦江南\n龙泉府：的卢 12/05 21:15\n本CD赤兔暂未刷新");
    }

    private TxMessageInfo buildRankTrialsDocApprovalMessage() {
        return buildTextMessage("试炼排行：长安城·花间游\n角色：好小鹿\n最高层数：80");
    }

    private TxMessageInfo buildTiebaItemRecordsDocApprovalMessage() {
        return buildTextMessage("贴吧物价：长安城\n内容：外观、金发、肩饰等交易记录");
    }

    private TxMessageInfo buildTradeDemonDocApprovalMessage() {
        return buildTextMessage("金币价格：长安城\n贴吧：935.00\n万宝楼：901.00\n日期：2025-12-02");
    }

    private TxMessageInfo buildTradeRecordsDocApprovalMessage() {
        return buildTextMessage("黑市物价：金发·璨月蝶心\n别名：狐金\n参考价：280.00\n日期：2016-04-28");
    }

    private TxMessageInfo buildTradeItemSearchDocApprovalMessage() {
        return buildTextMessage("搜索物品：十五夜观灯·南涧·标准\n别名：标准蓝观灯\n日期：2024-02-22");
    }

    private TxMessageInfo buildTradeItemRecordsDocApprovalMessage() {
        return buildTextMessage("物品价格：十五夜观灯·南涧·标准\n参考价：280.00\n日期：2024-02-22");
    }

    private TxMessageInfo buildBattleRecordsDocApprovalMessage() {
        return buildTextMessage("帮战记录：梦江南\n醉星河 对战 云上澜歌");
    }

    private TxMessageInfo buildMechCalculatorDocApprovalMessage() {
        return buildTextMessage("副本解密：18:02:53\n当前：北4 南0 西6 东9\n下一轮：18:15:00");
    }

    private TxMessageInfo buildDuowanStatisticsDocApprovalMessage() {
        return buildTextMessage("统战歪歪信息已查询。");
    }

    private TxMessageInfo buildTiebaRandomDocApprovalMessage() {
        return buildTextMessage("避雷团牌相关记录\n日期：2025-11-25");
    }

    private TxMessageInfo buildSaohuaRandomDocApprovalMessage() {
        return buildTextMessage("你说我菜也没用，我只会把你删了。");
    }

    private TxMessageInfo buildSaohuaContentDocApprovalMessage() {
        return buildTextMessage("爱你没有结果。");
    }

       /**
     * 绑定 服务器
     */
    private TxMessageInfo buildBindServerCalendarApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "乾坤一掷");
        return buildTextMessage("已绑定服务器：" + server);
    }

    /**
     * 日常
     * 日常 服务器
     * 日常 服务器 内容
     */
    private TxMessageInfo buildActiveCurrentApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "乾坤一掷");
        return buildTextMessage(server + " 今日日常：大战、战场、阵营任务已刷新。");
    }

    /**
     * 开服
     * 开服 服务器
     */
    private TxMessageInfo buildServerCheckApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "乾坤一掷");
        return buildTextMessage(server + " 当前状态：已开服。");
    }

    /**
     * 金价
     * 金价 服务器
     */
    private TxMessageInfo buildTradeDemonApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "乾坤一掷");
        return buildTextMessage(server + " 金价：约 1:1000。");
    }

    /**
     * 物价 内容
     */
    private TxMessageInfo buildTradeRecordApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), "");
        return buildTextMessage(value + " 近期物价已查询。");
    }

    /**
     * 骚话
     */
    private TxMessageInfo buildSaohuaRandomApprovalMessage(Map<String, String> params) {
        return buildTextMessage("愿你今日出奇遇，打本全红。");
    }

    /**
     * 更新
     * 公告
     * 更新公告
     */
    private TxMessageInfo buildNewsAnnounceApprovalMessage(Map<String, String> params) {
        return buildTextMessage("最新维护公告已获取。");
    }

    /**
     * 奇遇 内容
     * 奇遇 服务器 内容
     */
    private TxMessageInfo buildLuckAdventureApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), params.get("value1"), "");
        return buildTextMessage(value + " 奇遇记录已查询。");
    }

    /**
     * 汇总
     * 汇总 服务器
     */
    private TxMessageInfo buildLuckStatisticalApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "默认服务器");
        return buildTextMessage(server + " 奇遇统计已查询。");
    }

    /**
     * 战绩 内容
     * 战绩 服务器 内容
     */
    private TxMessageInfo buildMatchRecentApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), params.get("value1"), "");
        return buildTextMessage(value + " 名剑战绩已查询。");
    }

    /**
     * 装备 内容
     * 属性 内容
     * 装备 服务器 内容
     * 属性 服务器 内容
     */
    private TxMessageInfo buildRoleAttributeApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), params.get("value1"), "");
        return buildTextMessage(value + " 装备属性已查询。");
    }

    /**
     * 烟花 内容
     * 烟花 服务器 内容
     */
    private TxMessageInfo buildWatchRecordApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), params.get("value1"), "");
        return buildTextMessage(value + " 烟花记录已查询。");
    }

    /**
     * 招募
     * 招募 服务器
     * 招募 服务器 内容
     */
    private TxMessageInfo buildMemberRecruitApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), params.get("server1"), "默认服务器");
        return buildTextMessage(server + " 团队招募信息已查询。");
    }

    /**
     * 沙盘
     * 沙盘 服务器
     */
    private TxMessageInfo buildServerSandApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "默认服务器");
        return buildTextMessage(server + " 沙盘信息已查询。");
    }

    /**
     * 百战
     * 百战 服务器
     */
    private TxMessageInfo buildActiveMonsterApprovalMessage(Map<String, String> params) {
        String server = firstNotBlank(params.get("server"), "默认服务器");
        return buildTextMessage(server + " 百战首领已查询。");
    }

    /**
     * 奇穴 内容
     */
    private TxMessageInfo buildSchoolForceApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), "");
        return buildTextMessage(value + " 奇穴效果已查询。");
    }

    /**
     * 副本 内容
     * 副本 服务器 内容
     */
    private TxMessageInfo buildDungeonRecordApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), params.get("value1"), "");
        return buildTextMessage(value + " 副本记录已查询。");
    }

    /**
     * 水墨圈圈 内容
     */
    private TxMessageInfo buildLuckAdventurePsApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), "");
        return buildTextMessage(value + " 水墨圈圈记录已查询。");
    }

    /**
     * DPS 角色名
     * DPS 服务器 角色名
     * DPS 服务器 角色名 次数
     */
    private TxMessageInfo buildDpsComputeApprovalMessage(Map<String, String> params) {
        String roleName = firstNotBlank(params.get("roleName"), "");
        return buildTextMessage(roleName + " DPS 计算已完成。");
    }

    /**
     * 吃瓜 内容
     */
    private TxMessageInfo buildChiGuaApprovalMessage(Map<String, String> params) {
        String value = firstNotBlank(params.get("value"), "");
        return buildTextMessage(value + " 吃瓜信息已查询。");
    }

    private TxMessageInfo buildTextMessage(String content) {
        TxMessageInfo txMessageInfo = new TxMessageInfo();
        txMessageInfo.setMsg_type(0);
        txMessageInfo.setContent(content);
        return txMessageInfo;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
