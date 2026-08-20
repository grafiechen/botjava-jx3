package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.active.ActiveCelebritiesData;
import com.grafie.botjava.jx3.http.data.active.ActiveCurrentData;
import com.grafie.botjava.jx3.http.data.active.calendar.ActiveCalendarData;
import com.grafie.botjava.jx3.http.data.active.calendar.DataInfo;
import com.grafie.botjava.jx3.http.data.active.calendar.Today;
import com.grafie.botjava.jx3.http.data.exam.ExamAnswerData;
import com.grafie.botjava.jx3.http.data.match.MatchAwesomeData;
import com.grafie.botjava.jx3.http.data.match.MatchSchoolsData;
import com.grafie.botjava.jx3.http.data.fraud.detail.DetailInfo;
import com.grafie.botjava.jx3.http.data.fraud.detail.FraudDetailData;
import com.grafie.botjava.jx3.http.data.member.recruit.MemberRecruitData;
import com.grafie.botjava.jx3.http.data.member.teacher.MemberTeacherData;
import com.grafie.botjava.jx3.http.data.member.teacher.TeacherInfo;
import com.grafie.botjava.jx3.http.data.luck.LuckAdventureData;
import com.grafie.botjava.jx3.http.data.luck.unfinished.LuckUnfinishedData;
import com.grafie.botjava.jx3.http.data.news.NewsAllNewsData;
import com.grafie.botjava.jx3.http.data.news.NewsAnnounceData;
import com.grafie.botjava.jx3.http.data.official.ChatRecordsData;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.data.horse.HorseRanchData;
import com.grafie.botjava.jx3.http.data.horse.HorseRecordsData;
import com.grafie.botjava.jx3.http.data.home.HomeFurnitureData;
import com.grafie.botjava.jx3.http.data.home.HomeTravelData;
import com.grafie.botjava.jx3.http.data.acution.AcutionRecordsData;
import com.grafie.botjava.jx3.http.data.active.monster.ActiveMonsterData;
import com.grafie.botjava.jx3.http.data.active.monster.MonsterInfo;
import com.grafie.botjava.jx3.http.data.active.monster.OtherData;
import com.grafie.botjava.jx3.http.data.role.RoleDetailedData;
import com.grafie.botjava.jx3.http.data.role.RoleShowCardData;
import com.grafie.botjava.jx3.http.data.role.RoleShowRandomData;
import com.grafie.botjava.jx3.http.data.server.ServerAntiviceData;
import com.grafie.botjava.jx3.http.data.server.ServerMasterData;
import com.grafie.botjava.jx3.http.data.server.ServerEventData;
import com.grafie.botjava.jx3.http.data.server.sand.CastleInfo;
import com.grafie.botjava.jx3.http.data.server.sand.ServerSandData;
import com.grafie.botjava.jx3.http.data.role.monster.MonsterSkill;
import com.grafie.botjava.jx3.http.data.role.monster.RoleMonsterData;
import com.grafie.botjava.jx3.http.data.role.teamcd.RoleTeamCdListData;
import com.grafie.botjava.jx3.http.data.school.matirx.DescriptiveSkill;
import com.grafie.botjava.jx3.http.data.school.matirx.SchoolMatrixData;
import com.grafie.botjava.jx3.http.data.school.skill.SchoolSkillsData;
import com.grafie.botjava.jx3.http.data.school.skill.Skill;
import com.grafie.botjava.jx3.http.data.trade.record.SaleData;
import com.grafie.botjava.jx3.http.data.trade.record.TradeRecordData;
import com.grafie.botjava.jx3.http.data.tieba.TiebaItemRecordsData;
import com.grafie.botjava.jx3.http.data.trade.TradeDemonData;
import com.grafie.botjava.jx3.http.data.valuables.ValuablesStatisticalData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageActionResponseTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("imageCommands")
    void shouldDeclareTemplateAndData(ImageCase imageCase) throws Exception {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        properties.setTicket("ticket-test");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(imageCase.apiData());
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.doGetRequest(any(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, imageCase.regex().getMethodEnum()))
                .thenReturn(baseResult);
        when(requestUtil.loadRemoteImageDataUri(anyString())).thenReturn(Optional.empty());
        Jx3BaseAction action = instantiate(
                imageCase.regex().getBaseAction(), properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), imageCase.command(), imageCase.regex());

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals(imageCase.template(), response.getTemplateName());
        assertNotNull(response.getTemplateData());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals(imageCase.expectedServer(), templateData.get("server"));
        if (imageCase.expectedName() != null) {
            assertEquals(imageCase.expectedName(), templateData.get("name"));
        }
        if (imageCase.regex() == REGEX.MatchAwesome || imageCase.regex() == REGEX.MatchSchools) {
            assertEquals(33, templateData.get("mode"));
        }
        if (imageCase.regex() == REGEX.MatchSchools) {
            assertEquals(15D, templateData.get("maxValue"));
        }
        if (imageCase.regex() == REGEX.RankStatistical || imageCase.regex() == REGEX.RankTrials) {
            assertNotNull(templateData.get("time"));
        }
        if (imageCase.regex() == REGEX.SchoolSeniority) {
            assertEquals("万花", templateData.get("school"));
        }
        if (imageCase.regex() == REGEX.ChituRecords) {
            assertEquals("本日", templateData.get("mode"));
        }
        if (imageCase.regex() == REGEX.ChituWeekRecords) {
            assertEquals("本周", templateData.get("mode"));
        }
    }

    private static Stream<ImageCase> imageCommands() {
        Map<String, Object> objectData = Map.of("title", "测试数据");
        List<Map<String, Object>> listData = List.of(Map.of("name", "测试数据"));
        MemberRecruitData recruitData = new MemberRecruitData();
        recruitData.setServer("乾坤一掷");
        recruitData.setData(List.of());
        RoleTeamCdListData dungeonData = new RoleTeamCdListData();
        dungeonData.setServerName("乾坤一掷");
        dungeonData.setRoleName("加菲");
        dungeonData.setGlobalRoleId("GLOBAL-1");
        dungeonData.setData(List.of());
        RoleDetailedData roleData = new RoleDetailedData();
        roleData.setZoneName("电信区");
        roleData.setServerName("乾坤一掷");
        roleData.setRoleName("加菲");
        roleData.setForceName("万花");
        roleData.setBodyName("成男");
        roleData.setTongName("测试帮会");
        roleData.setCampName("中立");
        MatchAwesomeData rankingData = new MatchAwesomeData();
        rankingData.setZoneName("电信区");
        rankingData.setServerName("乾坤一掷");
        rankingData.setRoleName("加菲");
        rankingData.setForceName("万花");
        rankingData.setRankNum("1");
        rankingData.setScore("2700");
        rankingData.setUpNum("3");
        rankingData.setWinRate("68.5");
        MatchSchoolsData schoolData = new MatchSchoolsData();
        schoolData.setName("万花");
        schoolData.setLast(12);
        schoolData.setCurrent(15);
        OfficialQueryData.RankEntry rankEntry = new OfficialQueryData.RankEntry();
        rankEntry.setTongName("测试帮会");
        rankEntry.setCastleName("龙门荒漠");
        rankEntry.setMasterName("测试帮主");
        rankEntry.setNowCount(88);
        rankEntry.setMaxCount(100);
        rankEntry.setTotalScore(95270L);
        OfficialQueryData.RankStatistical rankData = new OfficialQueryData.RankStatistical();
        rankData.setZone("电信区");
        rankData.setServer("乾坤一掷");
        rankData.setName("名士五十强");
        rankData.setData(List.of(rankEntry));
        rankData.setTime(1784073600L);
        ValuablesStatisticalData dropData = new ValuablesStatisticalData();
        dropData.setZone("电信区");
        dropData.setServer("乾坤一掷");
        dropData.setName("玄晶");
        dropData.setRoleName("测试角色");
        dropData.setMapName("英雄会战弓月城");
        dropData.setTime(1784073600L);
        OfficialQueryData.SchoolSeniority seniorityData = new OfficialQueryData.SchoolSeniority();
        seniorityData.setZoneName("电信区");
        seniorityData.setServerName("乾坤一掷");
        seniorityData.setRoleName("测试角色");
        seniorityData.setForceName("万花");
        seniorityData.setSeniority(95270L);
        OfficialQueryData.TrialRankEntry trialEntry = new OfficialQueryData.TrialRankEntry();
        trialEntry.setMaxLevel(80);
        trialEntry.setRoleName("测试角色");
        trialEntry.setEquipScore(687028L);
        trialEntry.setTotalScore(35100L);
        OfficialQueryData.TrialRank trialData = new OfficialQueryData.TrialRank();
        trialData.setZone("电信区");
        trialData.setServer("乾坤一掷");
        trialData.setName("花间游");
        trialData.setData(List.of(trialEntry));
        trialData.setTime(1784073600L);
        LuckAdventureData adventureData = new LuckAdventureData();
        adventureData.setZone("电信区");
        adventureData.setServer("乾坤一掷");
        adventureData.setName("加菲");
        adventureData.setEvent("泛天河");
        adventureData.setTime(1784073600L);
        SaleData saleData = new SaleData();
        saleData.setZone("电信区");
        saleData.setServer("梦江南");
        saleData.setValue(3600);
        saleData.setSales(5);
        saleData.setDate("2026-07-15");
        TradeRecordData priceData = new TradeRecordData();
        priceData.setClassType("外观礼盒");
        priceData.setSubclass("同人盒子");
        priceData.setName("十五夜观灯·南涧·标准");
        priceData.setAlias("标准蓝观灯");
        priceData.setData(List.of(saleData));
        OfficialQueryData.TradeListing marketListing = new OfficialQueryData.TradeListing();
        marketListing.setZone("电信区");
        marketListing.setServer("乾坤一掷");
        marketListing.setValue(23500L);
        marketListing.setSale(4);
        marketListing.setDate("2026-07-15");
        OfficialQueryData.TradeRecords marketData = new OfficialQueryData.TradeRecords();
        marketData.setCategory("发型");
        marketData.setSubclass("金发");
        marketData.setName("金发·璨月蝶心");
        marketData.setAlias("狐金");
        marketData.setList(List.of(List.of(marketListing)));
        OfficialQueryData.BattleRecord battleData = new OfficialQueryData.BattleRecord();
        battleData.setZoneName("电信区");
        battleData.setServerName("乾坤一掷");
        battleData.setDeclaringTongName("醉星河");
        battleData.setAcceptingTongName("云上澜歌");
        battleData.setStartTime(1784073600L);
        battleData.setMatchDuration(3600L);
        battleData.setEndTime(1784077200L);
        OfficialQueryData.MineCartRecord mineRecord = new OfficialQueryData.MineCartRecord();
        mineRecord.setZone("电信区");
        mineRecord.setServer("剑胆琴心");
        mineRecord.setLeader("欧薏米·天鹅坪");
        mineRecord.setCampName("恶人谷");
        mineRecord.setCastle("赤焰关");
        mineRecord.setStatusText("保护期");
        OfficialQueryData.MineCart mineData = new OfficialQueryData.MineCart();
        mineData.setServer("剑胆琴心");
        mineData.setData(List.of(mineRecord));
        OfficialQueryData.ChituRecord dailyChitu = new OfficialQueryData.ChituRecord();
        dailyChitu.setServer("破阵子");
        dailyChitu.setMapName("黑戈壁");
        dailyChitu.setHorse("赤兔");
        dailyChitu.setDate("2026-07-15");
        OfficialQueryData.ChituWeekRecord weeklyChitu = new OfficialQueryData.ChituWeekRecord();
        weeklyChitu.setServer("剑胆琴心");
        weeklyChitu.setMapName("阴山大草原");
        weeklyChitu.setHorse("赤兔");
        weeklyChitu.setDate("2026-07-14");
        HorseRanchData ranchData = new HorseRanchData();
        ranchData.setZone("电信区");
        ranchData.setServer("梦江南");
        ranchData.setData(Map.of("黑戈壁", List.of("时间尚久，无法预知。")));
        ranchData.setNote("数据仅供参考，请以游戏内为准。");
        HorseRecordsData steedData = new HorseRecordsData();
        steedData.setServer("长安城");
        steedData.setZone("电信区");
        steedData.setMapName("龙泉府");
        steedData.setRefreshTime(1733490900L);
        steedData.setCaptureRoleName("慕深深");
        steedData.setCaptureTime(1733492702L);
        steedData.setAuctionRoleName("郭千千");
        steedData.setAuctionTime(1733494600L);
        steedData.setAuctionAmount("805万1514金");
        AcutionRecordsData auctionData = new AcutionRecordsData();
        auctionData.setZone("电信区");
        auctionData.setServer("唯我独尊");
        auctionData.setMapName("25人普通会战弓月城");
        auctionData.setRoleName("醉卧青苔");
        auctionData.setItemName("昆玉玄晶");
        auctionData.setItemAmount("0");
        auctionData.setTime(1764385748L);
        OtherData monsterExtra = new OtherData();
        monsterExtra.setName("无");
        monsterExtra.setList(List.of());
        monsterExtra.setDescription("无");
        MonsterInfo monsterInfo = new MonsterInfo();
        monsterInfo.setIndex(1);
        monsterInfo.setName("秦雷");
        monsterInfo.setSkill(List.of("积气法门", "霞月长针"));
        monsterInfo.setData(monsterExtra);
        ActiveMonsterData monsterData = new ActiveMonsterData();
        monsterData.setWeek("49");
        monsterData.setBoss("拓跋思南");
        monsterData.setStart(1764543600L);
        monsterData.setEnd(1765148400L);
        monsterData.setList(List.of(monsterInfo));
        MonsterSkill roleSkill = new MonsterSkill();
        roleSkill.setSkillName("空穴来风");
        roleSkill.setLeaderName("冯度");
        roleSkill.setCost(1);
        roleSkill.setColor(6);
        roleSkill.setLevel(10);
        roleSkill.setDeprecated(false);
        RoleMonsterData roleMonsterData = new RoleMonsterData();
        roleMonsterData.setZone("电信区");
        roleMonsterData.setServer("唯我独尊");
        roleMonsterData.setRoleName("夜温言");
        roleMonsterData.setSkillEnergy(237600L);
        roleMonsterData.setSkillStamina(236400L);
        roleMonsterData.setSkillCount(131);
        roleMonsterData.setUpdateTime(1763064756L);
        roleMonsterData.setSkillList(List.of(roleSkill));
        DescriptiveSkill matrixEffect = new DescriptiveSkill();
        matrixEffect.setLevel(1);
        matrixEffect.setName("一重粗识");
        matrixEffect.setDescription("阅历提高5%，声望提高5%，内功基础攻击力提高5%。");
        SchoolMatrixData matrixData = new SchoolMatrixData();
        matrixData.setName("花间游");
        matrixData.setSkillName("七绝逍遥阵");
        matrixData.setEffects(List.of(matrixEffect));
        Skill schoolSkill = new Skill();
        schoolSkill.setName("锋针");
        schoolSkill.setSimpleDescription("辅助技，非战斗状态下救治重伤的友方目标。");
        schoolSkill.setDescription("救治重伤的友方目标。");
        schoolSkill.setDistance("20尺");
        schoolSkill.setReleaseType("释放10秒");
        schoolSkill.setWeapon("笔类");
        SchoolSkillsData skillsData = new SchoolSkillsData();
        skillsData.setCategory("太素九针");
        skillsData.setSkills(List.of(schoolSkill));
        LuckUnfinishedData unfinishedData = new LuckUnfinishedData();
        unfinishedData.setName("三山四海");
        unfinishedData.setType("绝世奇遇");
        unfinishedData.setLevel(2);
        LuckAdventureData recentData = new LuckAdventureData();
        recentData.setZone("电信区");
        recentData.setServer("梦江南");
        recentData.setName("往矣");
        recentData.setEvent("侠者成歌");
        recentData.setTime(1764574651L);
        TeacherInfo mentorInfo = new TeacherInfo();
        mentorInfo.setRoleName("不见风澜");
        mentorInfo.setRoleLevel(130);
        mentorInfo.setCampName("恶人谷");
        mentorInfo.setTongName("夜寐");
        mentorInfo.setTongMasterName("有只鱼");
        mentorInfo.setBodyName("成男");
        mentorInfo.setForceName("刀宗");
        mentorInfo.setComment("找一个一起打名剑大会的师父");
        MemberTeacherData mentorData = new MemberTeacherData();
        mentorData.setZone("电信区");
        mentorData.setServer("长安城");
        mentorData.setType(2);
        mentorData.setTime(1764582410L);
        mentorData.setData(List.of(mentorInfo));
        CastleInfo castleInfo = new CastleInfo();
        castleInfo.setCastleName("金门关");
        castleInfo.setTongName("追梦烟雨");
        castleInfo.setMasterName("追梦的道士");
        castleInfo.setCampName("浩气盟");
        ServerSandData sandData = new ServerSandData();
        sandData.setZone("电信区");
        sandData.setServer("长安城");
        sandData.setUpdate(1764652005L);
        sandData.setData(List.of(castleInfo));
        ServerEventData factionEvent = new ServerEventData();
        factionEvent.setCampName("恶人谷");
        factionEvent.setFenxianName("梦江南");
        factionEvent.setFriendName("唯我独尊");
        factionEvent.setRoleName("欧薏米");
        factionEvent.setSeizeTime(1776240427L);
        ServerAntiviceData smiteEvent = new ServerAntiviceData();
        smiteEvent.setZone("电信区");
        smiteEvent.setServer("唯我独尊");
        smiteEvent.setMapName("银霜口");
        smiteEvent.setTime(1776227999L);
        OfficialQueryData.SkillRework reworkData = new OfficialQueryData.SkillRework();
        reworkData.setTitle("11月17日“山海源流”资料片武学调整");
        reworkData.setTime("2025-11-17 08:52:08");
        OfficialQueryData.SchoolFood foodData = new OfficialQueryData.SchoolFood();
        foodData.setSchool("万花");
        foodData.setKungfu("花间游");
        foodData.setColor("紫");
        foodData.setCategory("增强食品");
        foodData.setName("风语·灌汤包");
        foodData.setBoost("内功");
        HomeFurnitureData furnitureData = new HomeFurnitureData();
        furnitureData.setName("龙门香梦");
        furnitureData.setSource("大水南方令");
        furnitureData.setLimit(10);
        furnitureData.setQuality(1000);
        furnitureData.setView(7931);
        furnitureData.setPractical(3525);
        furnitureData.setHard(3525);
        furnitureData.setGeomantic(3525);
        furnitureData.setInteresting(3525);
        furnitureData.setTip("跳影如流泉，香梦过龙门。");
        HomeTravelData travelData = new HomeTravelData();
        travelData.setName("日晷");
        travelData.setSource("宠物游历");
        travelData.setProduce("万花");
        travelData.setLimit(3);
        travelData.setQuality(615);
        travelData.setView(484);
        travelData.setPractical(1743);
        travelData.setHard(500);
        travelData.setGeomantic(500);
        travelData.setInteresting(0);
        travelData.setTip("产出地图：万花");
        ActiveCurrentData dailyData = new ActiveCurrentData();
        dailyData.setDate("2025-12-03");
        dailyData.setWeek("三");
        dailyData.setWar("大战！英雄冰川宫宝库");
        dailyData.setBattle("浮香丘");
        dailyData.setLuck(List.of("丰丰"));
        Today calendarToday = new Today();
        calendarToday.setDate("2025-12-01");
        calendarToday.setWeek("一");
        DataInfo calendarDay = new DataInfo();
        calendarDay.setDate("2025-12-03");
        calendarDay.setWeek("三");
        calendarDay.setWar("英雄不染窟");
        ActiveCalendarData calendarData = new ActiveCalendarData();
        calendarData.setToday(calendarToday);
        calendarData.setData(List.of(calendarDay));
        ActiveCelebritiesData celebrityData = new ActiveCelebritiesData();
        celebrityData.setMapName("晟江");
        celebrityData.setEvent("恶霸出浴");
        celebrityData.setSite("白菰里");
        celebrityData.setDesc("公共任务：击退恶霸黄七。");
        celebrityData.setTime("12:56");
        OfficialQueryData.Flower flowerData = new OfficialQueryData.Flower();
        flowerData.setName("一级绣球花");
        flowerData.setColor("红，白，紫");
        flowerData.setPrice(1.5D);
        flowerData.setLine(List.of("6", "25", "18"));
        OfficialQueryData.HomeFlower homeFlowerData = new OfficialQueryData.HomeFlower();
        homeFlowerData.putServer("九寨沟·镜海", List.of(flowerData));
        RoleShowCardData cardData = new RoleShowCardData();
        cardData.setZone("电信区");
        cardData.setServer("乾坤一掷");
        cardData.setName("夜温言");
        cardData.setShowIndex(2);
        cardData.setShowActive(true);
        cardData.setStaticUrl("https://www.jx3api.com/cache/card.png");
        RoleShowRandomData randomCardData = new RoleShowRandomData();
        randomCardData.setZone("电信区");
        randomCardData.setServer("唯我独尊");
        randomCardData.setName("山色暮相依");
        randomCardData.setShowIndex(1);
        randomCardData.setAvatar("https://www.jx3api.com/cache/random-card.png");
        NewsAllNewsData newsData = new NewsAllNewsData();
        newsData.setType("公告");
        newsData.setTitle("夏日版本更新");
        newsData.setDate("2026-07-15 07:30:00");
        NewsAnnounceData announceData = new NewsAnnounceData();
        announceData.setType("维护");
        announceData.setTitle("例行维护公告");
        announceData.setDate("2026-07-15 06:00:00");
        DetailInfo fraudInfo = new DetailInfo();
        fraudInfo.setTitle("公开避雷记录");
        fraudInfo.setText("请在交易前再次核实相关信息。");
        fraudInfo.setTime(1_733_270_400L);
        FraudDetailData fraudData = new FraudDetailData();
        fraudData.setServer("乾坤一掷");
        fraudData.setTieba("剑网3吧");
        fraudData.setData(List.of(fraudInfo));
        TiebaItemRecordsData tiebaPrice = new TiebaItemRecordsData();
        tiebaPrice.setZone("电信区");
        tiebaPrice.setServer("乾坤一掷");
        tiebaPrice.setName("狐金");
        tiebaPrice.setContext("狐金近期成交参考");
        tiebaPrice.setReply(18L);
        tiebaPrice.setFloor(6);
        tiebaPrice.setTime(1_733_270_400L);
        TradeDemonData goldPrice = new TradeDemonData();
        goldPrice.setZone("电信区");
        goldPrice.setServer("乾坤一掷");
        goldPrice.setTieba("0.52");
        goldPrice.setWanbaolou("0.55");
        goldPrice.setDd373("0.53");
        goldPrice.setDate("2026-07-15");
        OfficialQueryData.MechNode currentNode = new OfficialQueryData.MechNode();
        currentNode.setNode("乾位");
        currentNode.setData("先点亮左侧机关");
        OfficialQueryData.MechNode nextNode = new OfficialQueryData.MechNode();
        nextNode.setNode("坎位");
        nextNode.setData("再触发中央石灯");
        OfficialQueryData.MechCalculator mechData = new OfficialQueryData.MechCalculator();
        mechData.setCurr(currentNode);
        mechData.setNext(nextNode);
        mechData.setTime("2026-07-15 15:00:00");
        mechData.setCdtn("完成当前机关后切换");
        OfficialQueryData.DuowanChannel channel = new OfficialQueryData.DuowanChannel();
        channel.setSnick("浩气盟统战");
        channel.setCampName("浩气盟");
        channel.setUsers(186);
        channel.setLimit(500);
        OfficialQueryData.DuowanStatistics duowanData = new OfficialQueryData.DuowanStatistics();
        duowanData.setServer("乾坤一掷");
        duowanData.setData(List.of(channel));
        ExamAnswerData examData = new ExamAnswerData();
        examData.setQuestion("古琴有几根弦？");
        examData.setAnswer("七根");
        ServerMasterData masterData = new ServerMasterData();
        masterData.setCenter("唯我独尊");
        masterData.setZone("电信区");
        masterData.setName("长安城");
        masterData.setAlias(List.of("长安", "电五长安"));
        masterData.setSlave(List.of("乾坤一掷", "斗转星移"));
        OfficialQueryData.ActiveNextEvent nextEvent = new OfficialQueryData.ActiveNextEvent();
        nextEvent.setZone("电信区");
        nextEvent.setServer("乾坤一掷");
        nextEvent.setStatus(1);
        nextEvent.setTime(1_764_574_651L);
        ChatRecordsData.ChatRecord chatRecord = new ChatRecordsData.ChatRecord();
        chatRecord.setZone("电信区");
        chatRecord.setServer("乾坤一掷");
        chatRecord.setRoleName("琉枫");
        chatRecord.setChannel("世界");
        chatRecord.setMessage("[跨服房间招募·25人普通会战弓月城]【千机】大小M 提升速 来T和奶");
        chatRecord.setTime(1_767_102_878L);
        ChatRecordsData chatRecords = new ChatRecordsData();
        chatRecords.setTotal(36);
        chatRecords.setList(List.of(chatRecord));

        return Stream.of(
                imageCase("活动日历", REGEX.ActiveCurrent, "日常 乾坤一掷", "活动日历",
                        dailyData, "乾坤一掷", null),
                imageCase("活动月历", REGEX.ActiveListCalendar, "月历 15", "活动月历",
                        calendarData, null, null),
                imageCase("行侠事件", REGEX.ActiveCelebrities, "行侠 楚天社", "行侠事件",
                        List.of(celebrityData), null, "楚天社"),
                imageCase("科举答题", REGEX.ExamAnswer, "科举 古琴有几根弦", "科举答题",
                        List.of(examData), null, null),
                imageCase("搜索区服", REGEX.ServerMaster, "区服 长安城", "搜索区服",
                        masterData, null, null),
                imageCase("家园鲜花", REGEX.HomeFlower, "鲜花 乾坤一掷 绣球花", "家园鲜花",
                        homeFlowerData, "乾坤一掷", "绣球花"),
                imageCase("角色名片", REGEX.RoleShowCard, "名片 乾坤一掷 夜温言", "角色名片",
                        cardData, "乾坤一掷", "夜温言"),
                imageCase("角色聊天", REGEX.ChatRecords, "角色聊天 乾坤一掷 琉枫 20 1", "角色聊天",
                        chatRecords, "乾坤一掷", null),
                imageCase("所有名片", REGEX.RoleShowCards, "所有名片 乾坤一掷 夜温言", "角色名片",
                        List.of(cardData), "乾坤一掷", "夜温言"),
                imageCase("随机名片", REGEX.RoleShowRandom, "随机名片 唯我独尊 萝莉 万花", "角色名片",
                        randomCardData, "唯我独尊", null),
                imageCase("缓存名片", REGEX.RoleShowCached, "缓存名片 乾坤一掷 夜温言", "角色名片",
                        cardData, "乾坤一掷", "夜温言"),
                imageCase("新闻资讯", REGEX.NewsAllNews, "新闻 3", "新闻资讯",
                        List.of(newsData), null, null),
                imageCase("维护公告", REGEX.NewsAnnounce, "更新公告", "新闻资讯",
                        List.of(announceData), null, null),
                imageCase("骗子查询", REGEX.FraudDetail, "骗子 570790267", "骗子查询",
                        List.of(fraudData), null, null),
                imageCase("贴吧物价", REGEX.TiebaItemRecords, "贴吧物价 乾坤一掷 狐金", "贴吧物价",
                        List.of(tiebaPrice), "乾坤一掷", "狐金"),
                imageCase("金币价格", REGEX.TradeDemon, "金价 乾坤一掷", "金币价格",
                        List.of(goldPrice), "乾坤一掷", null),
                imageCase("副本解密", REGEX.MechCalculator, "副本解密", "副本解密",
                        mechData, null, null),
                imageCase("统战歪歪", REGEX.DuowanStatistics, "统战 乾坤一掷", "统战歪歪",
                        List.of(duowanData), "乾坤一掷", null),
                imageCase("扶摇预测", REGEX.ActiveNextEvent, "扶摇 乾坤一掷", "扶摇预测",
                        List.of(nextEvent), "乾坤一掷", null),
                imageCase("角色信息", REGEX.RoleDetailed, "角色 乾坤一掷 加菲", "角色信息",
                        roleData, "乾坤一掷", "加菲"),
                imageCase("名剑排行", REGEX.MatchAwesome, "名剑排行 33 10", "名剑排行",
                        List.of(rankingData), null, null),
                imageCase("名剑统计", REGEX.MatchSchools, "名剑统计 33", "名剑统计",
                        List.of(schoolData), null, null),
                imageCase("本服榜单", REGEX.RankStatistical, "榜单 乾坤一掷 名士五十强", "本服榜单",
                        rankData, "乾坤一掷", "名士五十强"),
                imageCase("掉落统计", REGEX.ValuablesStatistical, "掉落 乾坤一掷 玄晶", "掉落统计",
                        List.of(dropData), "乾坤一掷", "玄晶"),
                imageCase("资历排行", REGEX.SchoolSeniority, "资历 乾坤一掷 万花", "资历排行",
                        List.of(seniorityData), "乾坤一掷", null),
                imageCase("试炼排行", REGEX.RankTrials, "试炼 乾坤一掷 花间游", "试炼排行",
                        trialData, "乾坤一掷", "花间游"),
                imageCase("角色奇遇", REGEX.LuckAdventure, "奇遇 乾坤一掷 加菲", "角色奇遇",
                        List.of(adventureData), "乾坤一掷", "加菲"),
                imageCase("物品价格", REGEX.TradeRecord, "物价 十五夜观灯", "物品价格",
                        priceData, "梦江南", "十五夜观灯"),
                imageCase("黑市物价", REGEX.TradeRecords, "黑市物价 乾坤一掷 狐金", "物品价格",
                        marketData, "乾坤一掷", "狐金"),
                imageCase("帮战记录", REGEX.BattleRecords, "帮战 乾坤一掷", "帮战记录",
                        List.of(battleData), "乾坤一掷", null),
                imageCase("关隘首领", REGEX.MineCart, "关隘首领", "关隘首领",
                        List.of(mineData), "全服", null),
                imageCase("本日赤兔", REGEX.ChituRecords, "本日赤兔", "赤兔记录",
                        List.of(dailyChitu), "全服", null),
                imageCase("本周赤兔", REGEX.ChituWeekRecords, "本周赤兔", "赤兔记录",
                        List.of(weeklyChitu), "全服", null),
                imageCase("马场刷新", REGEX.HorseEvent, "马场 梦江南", "马场刷新",
                        ranchData, "梦江南", null),
                imageCase("的卢记录", REGEX.HorseRecords, "的卢 长安城", "的卢记录",
                        List.of(steedData), "长安城", null),
                imageCase("阵营拍卖", REGEX.AuctionRecords, "拍卖 唯我独尊 玄晶", "阵营拍卖",
                        List.of(auctionData), "唯我独尊", "玄晶"),
                imageCase("百战首领", REGEX.ActiveMonster, "百战 梦江南", "百战首领",
                        monsterData, "梦江南", null),
                imageCase("角色百战", REGEX.RoleMonster, "角色百战 唯我独尊 夜温言", "角色百战",
                        roleMonsterData, "唯我独尊", null),
                imageCase("心法阵眼", REGEX.SchoolMatrix, "阵眼 花间游", "心法阵眼",
                        matrixData, null, "花间游"),
                imageCase("技能详情", REGEX.SchoolSkills, "技能 花间游", "技能详情",
                        List.of(skillsData), null, "花间游"),
                imageCase("未做奇遇", REGEX.LuckUnfinished, "未做奇遇 乾坤一掷 加菲", "未做奇遇",
                        List.of(unfinishedData), "乾坤一掷", "加菲"),
                imageCase("近期奇遇", REGEX.LuckRecent, "近期奇遇 梦江南", "近期奇遇",
                        List.of(recentData), "梦江南", null),
                imageCase("师徒系统", REGEX.MemberTeacher, "师徒 2 长安城 PVP", "师徒系统",
                        mentorData, "长安城", null),
                imageCase("阵营沙盘", REGEX.ServerSand, "沙盘 长安城", "阵营沙盘",
                        sandData, "长安城", null),
                imageCase("阵营事件", REGEX.ServerEvent, "阵营事件", "阵营事件",
                        List.of(factionEvent), null, null),
                imageCase("诛恶事件", REGEX.ServerAntivice, "诛恶事件", "诛恶事件",
                        List.of(smiteEvent), null, null),
                imageCase("技改记录", REGEX.SkillRework, "技改", "技改记录",
                        List.of(reworkData), null, null),
                imageCase("小药推荐", REGEX.SchoolFoods, "小药", "小药推荐",
                        List.of(foodData), null, null),
                imageCase("家园装饰", REGEX.HomeFurniture, "家具 龙门香梦", "家园装饰",
                        List.of(furnitureData), null, "龙门香梦"),
                imageCase("器物图谱", REGEX.HomeTravel, "器物 万花", "器物图谱",
                        List.of(travelData), null, "万花"),
                imageCase("角色装备", REGEX.RoleAttribute, "装备 乾坤一掷 加菲", "角色装备",
                        objectData, "乾坤一掷", "加菲"),
                imageCase("奇遇统计", REGEX.LuckStatistical, "汇总 乾坤一掷", "奇遇统计",
                        listData, "乾坤一掷", null),
                imageCase("奇遇汇总", REGEX.LuckCollect, "奇遇汇总 乾坤一掷 7", "奇遇汇总",
                        listData, "乾坤一掷", null),
                imageCase("名剑战绩", REGEX.MatchRecent, "战绩 乾坤一掷 加菲", "比赛记录",
                        objectData, "乾坤一掷", "加菲"),
                imageCase("烟花记录", REGEX.WatchRecord, "烟花 乾坤一掷 加菲", "烟花记录",
                        listData, "乾坤一掷", "加菲"),
                imageCase("奇穴详情", REGEX.SchoolForce, "奇穴 花间游", "奇穴信息",
                        listData, "梦江南", "花间游"),
                imageCase("团队招募", REGEX.MemberRecruit, "招募 乾坤一掷", "团队招募",
                        recruitData, "乾坤一掷", null),
                imageCase("副本进度", REGEX.DungeonRecord, "副本 乾坤一掷 加菲", "副本进度",
                        dungeonData, "乾坤一掷", "加菲")
        );
    }

    private static ImageCase imageCase(String name, REGEX regex, String command, String template,
                                       Object apiData, String server, String roleName) {
        return new ImageCase(name, regex, command, template, apiData, server, roleName);
    }

    private static Jx3BaseAction instantiate(Class<? extends Jx3BaseAction> type, ApiProperties properties,
                                             Jx3RequestUtil requestUtil, GroupConfigurationService groupConfigurationService)
            throws Exception {
        Constructor<? extends Jx3BaseAction> constructor = type.getConstructor(
                ApiProperties.class, Jx3RequestUtil.class, GroupConfigurationService.class);
        return constructor.newInstance(properties, requestUtil, groupConfigurationService);
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private record ImageCase(String name, REGEX regex, String command, String template, Object apiData,
                             String expectedServer, String expectedName) {
        @Override
        public String toString() {
            return name;
        }
    }
}
