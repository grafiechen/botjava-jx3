package com.grafie.botjava.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToImageUtlTest {

    @Test
    void shouldEncodeTemplateDataWithoutExecutableScriptBreakout() throws Exception {
        String payload = "</ScRiPt><script>window.compromised=true</script>";
        String html = HtmlToImageUtl.injectVueData("static/测试模板.html",
                "<html><head></head><body><div id=\"main\"></div></body></html>",
                Map.of("value", payload));

        assertFalse(html.contains(payload));
        assertFalse(html.contains("window.compromised=true"));
        assertTrue(html.contains("JSON.parse(new TextDecoder('utf-8')"));
        String expected = Base64.getEncoder().encodeToString(
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of("value", payload)));
        assertTrue(html.contains(expected));
    }

    @Test
    void shouldRenderVueTemplateToImageByTemplateName() throws Exception {
        Map<String, Object> data = Map.of(
                "title", "角色详情",
                "server", "乾坤一掷",
                "items", List.of("装备分数：12345", "心法：花间游")
        );

        String imagePath = HtmlToImageUtl.renderTemplateToImage("测试模板", data);
        Path output = Path.of(imagePath);

        assertTrue(Files.exists(output));
        assertTrue(output.startsWith(HtmlToImageUtl.getDefaultOutputDir()));
        assertTrue(Files.size(output) > 0);
        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 700);
        assertTrue(image.getHeight() > 100);
    }

    @Test
    void shouldRenderVueTemplateToImageByJsonData() throws Exception {
        Path output = Path.of("target", "test-output", "html-image", "测试模板-json.png");
        Files.createDirectories(output.getParent());
        String jsonData = """
                {
                  "title": "角色详情 JSON",
                  "server": "乾坤一掷",
                  "items": ["装备分数：12345", "心法：花间游"]
                }
                """;

        HtmlToImageUtl.renderTemplateJsonToImage("测试模板", jsonData, output.toString());

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 700);
        assertTrue(image.getHeight() > 100);
    }

    @Test
    void shouldRenderScriptStatusWithMultipleMongoMatches() throws Exception {
        Map<String, Object> data = Map.of(
                "server", "乾坤一掷",
                "roleName", "加菲",
                "matchCount", 2,
                "records", List.of(
                        Map.of("index", 1, "sections", List.of(
                                Map.of("name", "角色状态", "values", List.of(
                                        Map.of("name", "秘籍", "value", "是"),
                                        Map.of("name", "角色金币", "value", "128000"))),
                                Map.of("name", "日常任务", "values", List.of(
                                        Map.of("name", "每日签到", "value", "已完成"),
                                        Map.of("name", "日常大战", "value", "未完成"))))),
                        Map.of("index", 2, "sections", List.of(
                                Map.of("name", "角色状态", "values", List.of(
                                        Map.of("name", "秘籍", "value", "否"),
                                        Map.of("name", "角色金币", "value", "64000"))),
                                Map.of("name", "日常任务", "values", List.of(
                                        Map.of("name", "每日签到", "value", "已完成"),
                                        Map.of("name", "日常大战", "value", "已完成")))))
                )
        );
        Path output = Path.of("target", "test-output", "html-image", "脚本状态-多记录.png");
        Files.createDirectories(output.getParent());

        HtmlToImageUtl.renderTemplateToImage("脚本状态", data, output.toString());

        assertTrue(Files.exists(output));
        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 1000);
        assertTrue(image.getHeight() > 500);
    }
    @Test
    void shouldRenderDailyProgressTableWithDynamicFields() throws Exception {
        Map<String, Object> data = Map.of(
                "generatedAt", "2026-08-13 10:00",
                "roleCount", 2,
                "recordCount", 2,
                "columns", List.of(
                        Map.of("field", "每日签到任务", "name", "上次日常完成"),
                        Map.of("field", "角色金币", "name", "金币"),
                        Map.of("field", "精力", "name", "精力"),
                        Map.of("field", "侠行点", "name", "侠义")),
                "rows", List.of(
                        Map.of("server", "乾坤一掷", "roleName", "醉卧平沙", "values",
                                List.of("2026-08-11 17:02:44", "27655", "5258", "42579")),
                        Map.of("server", "梦江南", "roleName", "测试角色", "values",
                                List.of("-", "30000", "4000", "50000")))
        );
        Path output = Path.of("target", "test-output", "html-image", "日常进度-动态字段.png");
        Files.createDirectories(output.getParent());

        HtmlToImageUtl.renderTemplateToImage("日常进度", data, output.toString());

        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 1000);
        assertTrue(image.getHeight() > 500);
    }
    @Test
    void shouldRenderMigratedMainTemplates() throws Exception {
        assertRendered("角色信息", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", Map.of(
                        "zoneName", "电信区",
                        "serverName", "乾坤一掷",
                        "roleName", "测试角色",
                        "forceName", "万花",
                        "bodyName", "成男",
                        "tongName", "测试帮会",
                        "campName", "中立",
                        "personName", "名士"
                )
        ));

        assertRendered("名剑排行", Map.of(
                "mode", 33,
                "data", List.of(
                        Map.of(
                                "zoneName", "电信区", "serverName", "乾坤一掷",
                                "roleName", "测试角色一", "forceName", "万花",
                                "rankNum", "1", "score", "2780", "upNum", "3", "winRate", "68.5"
                        ),
                        Map.of(
                                "zoneName", "双线区", "serverName", "梦江南",
                                "roleName", "测试角色二", "forceName", "七秀",
                                "rankNum", "2", "score", "2710", "upNum", "1", "winRate", "65.2"
                        ),
                        Map.of(
                                "zoneName", "电信区", "serverName", "唯我独尊",
                                "roleName", "测试角色三", "forceName", "天策",
                                "rankNum", "3", "score", "2690", "upNum", "0", "winRate", "63.8"
                        )
                )
        ));

        assertRendered("名剑统计", Map.of(
                "mode", 33,
                "maxValue", 24,
                "data", List.of(
                        Map.of("name", "万花", "last", 18, "this", 24),
                        Map.of("name", "七秀", "last", 20, "this", 17),
                        Map.of("name", "天策", "last", 14, "this", 16),
                        Map.of("name", "少林", "last", 12, "this", 12),
                        Map.of("name", "纯阳", "last", 16, "this", 19),
                        Map.of("name", "藏剑", "last", 15, "this", 13)
                )
        ));

        assertRendered("资历排行", Map.of(
                "server", "乾坤一掷",
                "school", "万花",
                "data", List.of(
                        Map.of("zoneName", "电信区", "serverName", "乾坤一掷",
                                "roleName", "测试角色一", "forceName", "万花", "seniority", 195270),
                        Map.of("zoneName", "电信区", "serverName", "乾坤一掷",
                                "roleName", "测试角色二", "forceName", "万花", "seniority", 188430),
                        Map.of("zoneName", "双线区", "serverName", "梦江南",
                                "roleName", "测试角色三", "forceName", "万花", "seniority", 176820),
                        Map.of("zoneName", "电信区", "serverName", "唯我独尊",
                                "roleName", "测试角色四", "forceName", "万花", "seniority", 165900)
                )
        ));

        assertRendered("试炼排行", Map.of(
                "server", "乾坤一掷",
                "name", "花间游",
                "time", "2026-07-15 09:00:00",
                "data", Map.of(
                        "zone", "电信区",
                        "server", "乾坤一掷",
                        "name", "花间游",
                        "data", List.of(
                                Map.of("role_name", "测试角色一", "max_level", 80,
                                        "equip_score", 687028, "total_score", 35100),
                                Map.of("role_name", "测试角色二", "max_level", 78,
                                        "equip_score", 655210, "total_score", 33850),
                                Map.of("role_name", "测试角色三", "max_level", 76,
                                        "equip_score", 631900, "total_score", 32480),
                                Map.of("role_name", "测试角色四", "max_level", 75,
                                        "equip_score", 612340, "total_score", 31860)
                        )
                )
        ));

        assertRendered("角色奇遇", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "乾坤一掷", "name", "测试角色",
                                "event", "泛天河", "time", "2026-07-15 09:00:00"),
                        Map.of("zone", "电信区", "server", "乾坤一掷", "name", "测试角色",
                                "event", "阴阳两界", "time", "2026-06-28 21:16:05"),
                        Map.of("zone", "电信区", "server", "乾坤一掷", "name", "测试角色",
                                "event", "扶摇九天", "time", "2026-05-19 12:30:00"),
                        Map.of("zone", "电信区", "server", "乾坤一掷", "name", "测试角色",
                                "event", "三山四海", "time", "2026-04-03 08:08:08")
                )
        ));

        assertRendered("帮战记录", Map.of(
                "server", "乾坤一掷",
                "data", List.of(
                        Map.of("zoneName", "电信区", "serverName", "乾坤一掷",
                                "declaringTongName", "醉星河", "acceptingTongName", "云上澜歌",
                                "startTime", "2026-07-15 09:00:00", "endTime", "2026-07-15 10:00:00", "duration", "1小时"),
                        Map.of("zoneName", "电信区", "serverName", "乾坤一掷",
                                "declaringTongName", "烽火九州", "acceptingTongName", "江月照初人",
                                "startTime", "2026-07-14 20:00:00", "endTime", "2026-07-15 01:00:00", "duration", "5小时"),
                        Map.of("zoneName", "双线区", "serverName", "梦江南",
                                "declaringTongName", "山河同归", "acceptingTongName", "故人入梦",
                                "startTime", "2026-07-13 19:30:00", "endTime", "2026-07-13 21:00:00", "duration", "1小时30分钟"),
                        Map.of("zoneName", "电信区", "serverName", "唯我独尊",
                                "declaringTongName", "长风破浪", "acceptingTongName", "月满西楼",
                                "startTime", "2026-07-12 18:00:00", "endTime", "2026-07-12 20:15:00", "duration", "2小时15分钟")
                )
        ));

        assertRendered("关隘首领", Map.of(
                "server", "全服",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "剑胆琴心", "leader", "欧薏米·天鹅坪",
                                "campName", "恶人谷", "castle", "赤焰关", "statusText", "保护期"),
                        Map.of("zone", "双线区", "server", "梦江南", "leader", "洛水长歌",
                                "campName", "浩气盟", "castle", "逐风关", "statusText", "可争夺"),
                        Map.of("zone", "电信区", "server", "乾坤一掷", "leader", "山河故人",
                                "campName", "恶人谷", "castle", "霜月关", "statusText", "交战中"),
                        Map.of("zone", "双线区", "server", "唯我独尊", "leader", "长安旧梦",
                                "campName", "浩气盟", "castle", "镇南关", "statusText", "已结束")
                )
        ));

        assertRendered("本服榜单", Map.of(
                "server", "乾坤一掷",
                "name", "名士五十强",
                "time", "2026-07-15 09:00:00",
                "data", Map.of(
                        "zone", "电信区",
                        "server", "乾坤一掷",
                        "name", "名士五十强",
                        "data", List.of(
                                Map.of(
                                        "tong_name", "测试帮会一", "castle_name", "龙门荒漠",
                                        "master_name", "测试帮主一", "now_count", 88,
                                        "max_count", 100, "total_score", 95270
                                ),
                                Map.of(
                                        "tong_name", "测试帮会二", "castle_name", "黑龙沼",
                                        "master_name", "测试帮主二", "now_count", 76,
                                        "max_count", 100, "total_score", 88720
                                ),
                                Map.of(
                                        "tong_name", "测试帮会三", "castle_name", "白龙口",
                                        "master_name", "测试帮主三", "now_count", 64,
                                        "max_count", 100, "total_score", 81350
                                )
                        )
                )
        ));

        assertRendered("掉落统计", Map.of(
                "server", "乾坤一掷",
                "name", "玄晶",
                "data", List.of(
                        Map.of(
                                "name", "沉沙玄晶", "role_name", "测试角色一",
                                "map_name", "英雄会战弓月城", "time", "2026-07-15 08:30:00"
                        ),
                        Map.of(
                                "name", "沉沙玄晶", "role_name", "测试角色二",
                                "map_name", "英雄冷龙峰", "time", "2026-07-14 22:10:00"
                        ),
                        Map.of(
                                "name", "沉沙玄晶", "role_name", "测试角色三",
                                "map_name", "英雄河阳之战", "time", "2026-07-14 20:05:00"
                        ),
                        Map.of(
                                "name", "沉沙玄晶", "role_name", "测试角色四",
                                "map_name", "英雄达摩洞", "time", "2026-07-13 19:40:00"
                        )
                )
        ));

        assertRendered("团队招募", Map.of(
                "server", "乾坤一掷",
                "time", "2026-07-10 19:20",
                "num", 1,
                "data", List.of(Map.of(
                        "activity", "25人英雄会战弓月城",
                        "level", "满级",
                        "leader", "测试团长",
                        "number", "12/25",
                        "content", "来输出和治疗",
                        "createTime", "19:00"
                ))
        ));

        assertRendered("角色装备", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", Map.of(
                        "kungfu", "花间游",
                        "score", 12345,
                        "info", List.of(Map.of("name", "气血", "value", "100000")),
                        "equip", List.of(Map.of(
                                "color", "#7b68ee",
                                "icon", "img/star.png",
                                "name", "测试装备",
                                "quality", "999",
                                "modifyType", "精炼六级",
                                "source", "测试来源",
                                "strengthLevel", 3,
                                "permanentEnchant", "测试附魔",
                                "fiveStone", List.of("img/star.png")
                        )),
                        "qixue", List.of(Map.of("icon", "img/star.png", "name", "测试奇穴"))
                )
        ));

        assertRendered("副本进度", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "globalId", "123456",
                "data", List.of(Map.of(
                        "name", "英雄测试副本",
                        "type", "25人",
                        "bosses", List.of(
                                Map.of("boss_name", "首领一", "finished", true),
                                Map.of("boss_name", "首领二", "finished", false)
                        )
                ))
        ));

        assertRendered("奇穴信息", Map.of(
                "name", "花间游",
                "data", List.of(Map.of(
                        "level", 1,
                        "data", List.of(
                                Map.of("icon", "img/star.png", "name", "测试奇穴一", "kind", "输出", "desc", "测试描述一"),
                                Map.of("icon", "img/star.png", "name", "测试奇穴二", "kind", "辅助", "desc", "测试描述二")
                        )
                ))
        ));

        assertRendered("奇遇统计", Map.of(
                "server", "乾坤一掷",
                "name", "七天统计",
                "data", List.of(Map.of("name", "阴阳两界", "time", "2026-07-12", "day", "1天"))
        ));

        assertRendered("奇遇汇总", Map.of(
                "server", "乾坤一掷",
                "data", List.of(Map.of(
                        "event", "阴阳两界",
                        "count", 3,
                        "data", Map.of("name", "测试角色", "time", "2026-07-12")
                ))
        ));

        assertRendered("比赛记录", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", Map.of(
                        "camp", "中立",
                        "performance", Map.of("3v3", Map.of(
                                "grade", 12, "totalCount", 20, "mvpCount", 5,
                                "winCount", 12, "winRate", 60, "mmr", 2400, "ranking", 100
                        ))
                )
        ));

        assertRendered("烟花记录", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", List.of(Map.of(
                        "time", "2026-07-12 00:00", "map", "扬州", "name", "真橙之心",
                        "sender", "甲", "recipient", "乙", "times", 1
                ))
        ));

        assertRendered("物品价格", Map.of(
                "mode", "物品价格",
                "server", "乾坤一掷",
                "name", "十五夜观灯",
                "data", Map.of(
                        "name", "十五夜观灯",
                        "alias", "蓝观灯",
                        "classType", "外观",
                        "subclass", "背部挂件",
                        "level", 1,
                        "desc", "用于测试的物品价格说明。",
                        "date", "2026-07-10",
                        "data", List.of(
                                List.of(
                                        Map.of("date", "2026-07-01", "server", "乾坤一掷", "value", 260, "sales", 5),
                                        Map.of("date", "2026-07-02", "server", "乾坤一掷", "value", 270, "sales", 5),
                                        Map.of("date", "2026-07-03", "server", "乾坤一掷", "value", 280, "sales", 5)
                                ),
                                List.of(
                                        Map.of("date", "2026-07-04", "server", "梦江南", "value", 290, "sales", 1),
                                        Map.of("date", "2026-07-05", "server", "梦江南", "value", 300, "sales", 7)
                                ),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(Map.of("date", "2026-07-06", "server", "乾坤一掷", "value", 310, "sales", 5))
                        )
                )
        ));

        assertRendered("物品价格", "黑市物价", Map.of(
                "mode", "黑市物价",
                "server", "乾坤一掷",
                "name", "狐金",
                "data", Map.of(
                        "class", "发型",
                        "subclass", "金发",
                        "name", "金发·璨月蝶心",
                        "alias", "狐金",
                        "value", "280.00",
                        "desc", "不绑定限时发售外观，用于验证黑市价格记录。",
                        "date", "2016-04-28",
                        "list", List.of(
                                List.of(
                                        Map.of("date", "2026-07-10", "zone", "电信区", "server", "乾坤一掷", "value", 23000, "sale", 4),
                                        Map.of("date", "2026-07-11", "zone", "电信区", "server", "乾坤一掷", "value", 23500, "sale", 4),
                                        Map.of("date", "2026-07-12", "zone", "电信区", "server", "绝代天骄", "value", 22800, "sale", 1)
                                ),
                                List.of(
                                        Map.of("date", "2026-07-13", "zone", "双线区", "server", "梦江南", "value", 24000, "sale", 5),
                                        Map.of("date", "2026-07-14", "zone", "双线区", "server", "梦江南", "value", 23800, "sale", 1),
                                        Map.of("date", "2026-07-15", "zone", "双线区", "server", "长安城", "value", 24500, "sale", 7)
                                )
                        )
                )
        ));

        assertRendered("赤兔记录", "本日赤兔", Map.of(
                "mode", "本日",
                "server", "全服",
                "data", List.of(
                        Map.of("server", "破阵子", "mapName", "黑戈壁", "horse", "赤兔", "date", "2026-07-15"),
                        Map.of("server", "剑胆琴心", "mapName", "阴山大草原", "horse", "赤兔", "date", "2026-07-15"),
                        Map.of("server", "梦江南", "mapName", "鲲鹏岛", "horse", "赤兔", "date", "2026-07-14")
                )
        ));

        assertRendered("赤兔记录", "本周赤兔", Map.of(
                "mode", "本周",
                "server", "全服",
                "data", List.of(
                        Map.of("server", "乾坤一掷", "mapName", "黑戈壁", "horse", "赤兔", "date", "2026-07-13"),
                        Map.of("server", "唯我独尊", "mapName", "阴山大草原", "horse", "赤兔", "date", "2026-07-12"),
                        Map.of("server", "长安城", "mapName", "鲲鹏岛", "horse", "赤兔", "date", "2026-07-11")
                )
        ));

        assertRendered("马场刷新", Map.of(
                "zone", "电信区",
                "server", "梦江南",
                "note", "本CD内赤兔暂未刷新，还请关注！\n\n数据仅供参考，请以游戏内为准。",
                "data", List.of(
                        Map.of("mapName", "黑戈壁", "predictions", List.of("时间尚久，无法预知。")),
                        Map.of("mapName", "阴山大草原", "predictions", List.of("赤兔（07/15 19:30）", "里飞沙（07/15 21:10）")),
                        Map.of("mapName", "鲲鹏岛", "predictions", List.of("时间尚久，无法预知。")),
                        Map.of("mapName", "龙泉府 / 进图（21:10）", "predictions", List.of("的卢（07/15 21:15）"))
                )
        ));

        assertRendered("的卢记录", Map.of(
                "server", "长安城",
                "data", List.of(
                        Map.ofEntries(
                                Map.entry("zone", "电信区"), Map.entry("server", "长安城"),
                                Map.entry("mapName", "龙泉府"), Map.entry("refreshTime", "2024-12-06 21:15:00"),
                                Map.entry("captureRoleName", "慕深深"), Map.entry("captureCampName", "恶人谷"),
                                Map.entry("captureTime", "2024-12-06 21:45:02"), Map.entry("auctionRoleName", "郭千千"),
                                Map.entry("auctionCampName", "恶人谷"), Map.entry("auctionTime", "2024-12-06 22:16:40"),
                                Map.entry("auctionAmount", "805万1514金"), Map.entry("startTime", "2024-12-02 07:00:00"),
                                Map.entry("endTime", "2024-12-08 07:00:00")
                        ),
                        Map.ofEntries(
                                Map.entry("zone", "电信区"), Map.entry("server", "长安城"),
                                Map.entry("mapName", "黑戈壁"), Map.entry("refreshTime", "2024-11-29 20:30:00"),
                                Map.entry("captureRoleName", "测试角色甲"), Map.entry("captureCampName", "浩气盟"),
                                Map.entry("captureTime", "2024-11-29 20:42:10"), Map.entry("auctionRoleName", "测试角色乙"),
                                Map.entry("auctionCampName", "浩气盟"), Map.entry("auctionTime", "2024-11-29 21:05:30"),
                                Map.entry("auctionAmount", "760万金"), Map.entry("startTime", "2024-11-25 07:00:00"),
                                Map.entry("endTime", "2024-12-01 07:00:00")
                        ),
                        Map.ofEntries(
                                Map.entry("zone", "电信区"), Map.entry("server", "长安城"),
                                Map.entry("mapName", "阴山大草原"), Map.entry("refreshTime", "2024-11-22 19:45:00"),
                                Map.entry("captureRoleName", "测试角色丙"), Map.entry("captureCampName", "恶人谷"),
                                Map.entry("captureTime", "2024-11-22 20:01:20"), Map.entry("auctionRoleName", "测试角色丁"),
                                Map.entry("auctionCampName", "恶人谷"), Map.entry("auctionTime", "2024-11-22 20:30:00"),
                                Map.entry("auctionAmount", "812万3000金"), Map.entry("startTime", "2024-11-18 07:00:00"),
                                Map.entry("endTime", "2024-11-24 07:00:00")
                        )
                )
        ));

        assertRendered("阵营拍卖", Map.of(
                "server", "唯我独尊",
                "name", "玄晶",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "唯我独尊", "mapName", "25人普通会战弓月城",
                                "roleName", "醉卧青苔", "campName", "恶人谷", "itemName", "昆玉玄晶",
                                "itemAmount", "1", "time", "2025-11-29 10:29:08"),
                        Map.of("zone", "电信区", "server", "唯我独尊", "mapName", "25人英雄河阳之战",
                                "roleName", "测试角色甲", "campName", "浩气盟", "itemName", "沉沙玄晶",
                                "itemAmount", "1", "time", "2025-11-28 22:15:30"),
                        Map.of("zone", "电信区", "server", "唯我独尊", "mapName", "25人普通范阳夜变",
                                "roleName", "测试角色乙", "campName", "恶人谷", "itemName", "醉月玄晶",
                                "itemAmount", "2", "time", "2025-11-27 20:08:16"),
                        Map.of("zone", "电信区", "server", "唯我独尊", "mapName", "25人英雄达摩洞",
                                "roleName", "测试角色丙", "campName", "浩气盟", "itemName", "浮屠玄晶",
                                "itemAmount", "1", "time", "2025-11-26 21:42:05")
                )
        ));

        assertRendered("百战首领", Map.of(
                "server", "梦江南",
                "week", "49",
                "boss", "拓跋思南",
                "start", "2025-12-01 07:00:00",
                "end", "2025-12-08 07:00:00",
                "data", List.of(
                        Map.of("index", 1, "name", "秦雷",
                                "skills", List.of("积气法门", "霞月长针", "暗狼袭爪", "黑煞落贪狼", "皓莲望月"),
                                "extraName", "无", "effects", List.of(), "description", "无"),
                        Map.of("index", 2, "name", "拓跋思南",
                                "skills", List.of("剑气纵横", "破空式", "镇山河", "问鼎天下"),
                                "extraName", "剑意激荡", "effects", List.of("攻击提高", "移动速度提高"),
                                "description", "首领进入激怒状态后，需优先处理场地机制。"),
                        Map.of("index", 3, "name", "萧沙",
                                "skills", List.of("流星赶月", "风卷残云", "定军"),
                                "extraName", "沙海迷踪", "effects", List.of("视野缩小"),
                                "description", "注意躲避连续范围攻击。")
                )
        ));

        assertRendered("角色百战", Map.of(
                "zone", "电信区",
                "server", "唯我独尊",
                "roleName", "夜温言@长安城",
                "skillEnergy", 237600,
                "skillStamina", 236400,
                "skillCount", 131,
                "updateTime", "2025-11-14 04:12:36",
                "skills", List.of(
                        Map.of("name", "空穴来风", "leaderName", "冯度", "cost", 1, "color", 6, "level", 10, "deprecated", false),
                        Map.of("name", "雷霆震怒", "leaderName", "拓跋思南", "cost", 2, "color", 4, "level", 8, "deprecated", false),
                        Map.of("name", "断水流", "leaderName", "萧沙", "cost", 1, "color", 3, "level", 7, "deprecated", false),
                        Map.of("name", "旧式机关术", "leaderName", "秦雷", "cost", 3, "color", 2, "level", 5, "deprecated", true),
                        Map.of("name", "风卷残云", "leaderName", "萧沙", "cost", 2, "color", 5, "level", 9, "deprecated", false),
                        Map.of("name", "皓莲望月", "leaderName", "秦雷", "cost", 1, "color", 1, "level", 6, "deprecated", false)
                )
        ));

        assertRendered("心法阵眼", Map.of(
                "name", "花间游",
                "skillName", "七绝逍遥阵",
                "effects", List.of(
                        Map.of("level", 1, "name", "一重粗识", "description", "阅历提高5%，声望提高5%，内功基础攻击力提高5%。"),
                        Map.of("level", 2, "name", "二重略懂", "description", "内功破防等级提高，阵眼成员获得额外会心效果。"),
                        Map.of("level", 3, "name", "三重巧熟", "description", "施展招式后有一定概率使小队成员内功攻击提高。"),
                        Map.of("level", 4, "name", "四重精妙", "description", "阵眼范围内成员的无双等级和会心效果进一步提高。"),
                        Map.of("level", 5, "name", "五重游刃", "description", "阵眼完整激活时获得最终增益效果。")
                )
        ));

        assertRendered("技能详情", Map.of(
                "name", "离经易道",
                "groups", List.of(
                        Map.of("category", "太素九针", "skills", List.of(
                                skillData("锋针", "辅助技，非战斗状态下救治重伤的友方目标。",
                                        "救治重伤的友方目标，使其恢复部分气血值和内力值。", "第其身而锋其末。",
                                        "无调息时间", "", "20尺", "技能", "万花", "释放10秒", "笔类"),
                                skillData("长针", "治疗技，持续运功后恢复友方目标气血。",
                                        "对友方目标施展长针，回复大量气血值。", "长针入脉，生息不绝。",
                                        "调息8秒", "3%内力", "20尺", "技能", "万花", "运功2秒", "笔类")
                        )),
                        Map.of("category", "养心诀", "skills", List.of(
                                skillData("清心静气", "辅助技，提高友方目标气血最大值。",
                                        "使友方目标获得清心静气效果。", "清心以静气。", "无调息时间",
                                        "1%内力", "20尺", "技能", "万花", "瞬间释放", "笔类")
                        ))
                )
        ));

        assertRendered("未做奇遇", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", List.of(
                        Map.of("name", "三山四海", "type", "绝世奇遇", "level", 2),
                        Map.of("name", "阴阳两界", "type", "绝世奇遇", "level", 2),
                        Map.of("name", "扶摇九天", "type", "普通奇遇", "level", 1),
                        Map.of("name", "塞外宝驹", "type", "宠物奇遇", "level", 1),
                        Map.of("name", "茶馆奇缘", "type", "普通奇遇", "level", 1),
                        Map.of("name", "护佑苍生", "type", "绝世奇遇", "level", 3)
                )
        ));

        assertRendered("近期奇遇", Map.of(
                "server", "梦江南",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "梦江南", "name", "往矣", "event", "侠者成歌", "time", "2025-12-01 15:37:31"),
                        Map.of("zone", "电信区", "server", "梦江南", "name", "春风十里", "event", "阴阳两界", "time", "2025-12-01 14:12:09"),
                        Map.of("zone", "电信区", "server", "梦江南", "name", "山河故人", "event", "三山四海", "time", "2025-12-01 12:48:22")
                )
        ));
    }

    @Test
    void shouldRenderActiveMonsterTemplate() throws Exception {
        assertRendered("百战首领", Map.of(
                "server", "梦江南",
                "week", "49",
                "boss", "拓跋思南",
                "start", "2025-12-01 07:00:00",
                "end", "2025-12-08 07:00:00",
                "data", List.of(
                        Map.of("index", 1, "name", "秦雷",
                                "skills", List.of("积气法门", "霞月长针", "暗狼袭爪", "黑煞落贪狼", "皓莲望月"),
                                "extraName", "无", "effects", List.of(), "description", "无"),
                        Map.of("index", 2, "name", "拓跋思南",
                                "skills", List.of("剑气纵横", "破空式", "镇山河", "问鼎天下"),
                                "extraName", "剑意激荡", "effects", List.of("攻击提高", "移动速度提高"),
                                "description", "首领进入激怒状态后，需优先处理场地机制。")
                )
        ));
    }

    @Test
    void shouldRenderRoleMonsterTemplate() throws Exception {
        assertRendered("角色百战", Map.of(
                "zone", "电信区",
                "server", "唯我独尊",
                "roleName", "夜温言@长安城",
                "skillEnergy", 237600,
                "skillStamina", 236400,
                "skillCount", 131,
                "updateTime", "2025-11-14 04:12:36",
                "skills", List.of(
                        Map.of("name", "空穴来风", "leaderName", "冯度", "cost", 1, "color", 6, "level", 10, "deprecated", false),
                        Map.of("name", "雷霆震怒", "leaderName", "拓跋思南", "cost", 2, "color", 4, "level", 8, "deprecated", false),
                        Map.of("name", "旧式机关术", "leaderName", "秦雷", "cost", 3, "color", 2, "level", 5, "deprecated", true)
                )
        ));
    }

    @Test
    void shouldRenderSchoolMatrixAndSkillsTemplates() throws Exception {
        assertRendered("心法阵眼", Map.of(
                "name", "花间游",
                "skillName", "七绝逍遥阵",
                "effects", List.of(
                        Map.of("level", 1, "name", "一重粗识", "description", "阅历提高5%，声望提高5%，内功基础攻击力提高5%。"),
                        Map.of("level", 2, "name", "二重略懂", "description", "内功破防等级提高，阵眼成员获得额外会心效果。")
                )
        ));
        assertRendered("技能详情", Map.of(
                "name", "离经易道",
                "groups", List.of(Map.of("category", "太素九针", "skills", List.of(
                        skillData("锋针", "辅助技，非战斗状态下救治重伤的友方目标。",
                                "救治重伤的友方目标，使其恢复部分气血值和内力值。", "第其身而锋其末。",
                                "无调息时间", "", "20尺", "技能", "万花", "释放10秒", "笔类")
                )))
        ));
    }

    @Test
    void shouldRenderUnfinishedAndRecentAdventureTemplates() throws Exception {
        assertRendered("未做奇遇", Map.of(
                "server", "乾坤一掷",
                "name", "测试角色",
                "data", List.of(
                        Map.of("name", "三山四海", "type", "绝世奇遇", "level", 2),
                        Map.of("name", "扶摇九天", "type", "普通奇遇", "level", 1)
                )
        ));
        assertRendered("近期奇遇", Map.of(
                "server", "梦江南",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "梦江南", "name", "往矣", "event", "侠者成歌", "time", "2025-12-01 15:37:31"),
                        Map.of("zone", "电信区", "server", "梦江南", "name", "春风十里", "event", "阴阳两界", "time", "2025-12-01 14:12:09")
                )
        ));
    }

    @Test
    void shouldRenderMentorAndFactionTemplates() throws Exception {
        assertRendered("师徒系统", Map.of(
                "zone", "电信区", "server", "长安城", "time", "2025-12-01 17:46:50",
                "data", List.of(
                        Map.of("roleName", "不见风澜", "roleLevel", 130, "campName", "恶人谷",
                                "tongName", "夜寐", "tongMasterName", "有只鱼", "bodyName", "成男",
                                "forceName", "刀宗", "comment", "找个一起打名剑大会的师父"),
                        Map.of("roleName", "江湖旧梦", "roleLevel", 130, "campName", "浩气盟",
                                "tongName", "青山不改", "tongMasterName", "归云", "bodyName", "成女",
                                "forceName", "万花", "comment", "日常上线，希望找固定师徒一起做任务")
                )
        ));
        assertRendered("阵营沙盘", Map.of(
                "zone", "电信区", "server", "长安城", "update", "2025-12-02 13:06:45",
                "data", List.of(
                        Map.of("castleName", "金门关", "tongName", "追梦烟雨", "masterName", "追梦的道士", "campName", "浩气盟"),
                        Map.of("castleName", "逐风关", "tongName", "山河故人", "masterName", "洛水长歌", "campName", "恶人谷")
                )
        ));
        assertRendered("阵营事件", Map.of(
                "scope", "全服",
                "data", List.of(
                        Map.of("campName", "恶人谷", "fenxianName", "梦江南", "friendName", "唯我独尊",
                                "roleName", "欧薏米·天鹅坪", "seizeTime", "2026-04-15 15:27:07"),
                        Map.of("campName", "浩气盟", "fenxianName", "长安城", "friendName", "乾坤一掷",
                                "roleName", "江湖旧梦", "seizeTime", "2026-04-15 14:18:20")
                )
        ));
        assertRendered("诛恶事件", Map.of(
                "scope", "全服",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "唯我独尊", "mapName", "银霜口", "time", "2026-04-15 12:13:19"),
                        Map.of("zone", "双线区", "server", "梦江南", "mapName", "龙泉府", "time", "2026-04-15 11:42:05")
                )
        ));
    }

    @Test
    void shouldRenderHomeAndGuideTemplates() throws Exception {
        assertRendered("技改记录", Map.of("data", List.of(
                Map.of("title", "11月17日“山海源流”资料片武学调整", "time", "2025-11-17 08:52:08"),
                Map.of("title", "10月30日门派武学平衡性调整公告", "time", "2025-10-30 09:15:00"),
                Map.of("title", "10月16日部分心法技能效果调整", "time", "2025-10-16 07:30:00")
        )));
        assertRendered("小药推荐", Map.of("data", List.of(
                Map.of("school", "万花", "kungfu", "花间游", "color", "紫", "category", "增强食品", "name", "风语·灌汤包", "boost", "内功"),
                Map.of("school", "七秀", "kungfu", "冰心诀", "color", "蓝", "category", "增强药品", "name", "聚魂丹", "boost", "会心"),
                Map.of("school", "天策", "kungfu", "傲血战意", "color", "紫", "category", "辅助食品", "name", "玉笛谁家听落梅", "boost", "外功")
        )));
        assertRendered("家园装饰", Map.of(
                "name", "龙门香梦",
                "data", List.of(Map.ofEntries(
                        Map.entry("name", "龙门香梦"), Map.entry("source", "大水南方令"),
                        Map.entry("limit", 10), Map.entry("quality", 1000), Map.entry("view", 7931),
                        Map.entry("practical", 3525), Map.entry("hard", 3525), Map.entry("geomantic", 3525),
                        Map.entry("interesting", 3525), Map.entry("produce", ""),
                        Map.entry("tip", "跳影如流泉，香梦过龙门。山中逍遥客，化为唤雨人。")
                ))
        ));
        assertRendered("器物图谱", Map.of(
                "name", "万花",
                "data", List.of(Map.ofEntries(
                        Map.entry("name", "日晷"), Map.entry("source", "宠物游历"),
                        Map.entry("limit", 3), Map.entry("quality", 615), Map.entry("view", 484),
                        Map.entry("practical", 1743), Map.entry("hard", 500), Map.entry("geomantic", 500),
                        Map.entry("interesting", 0), Map.entry("produce", "万花"), Map.entry("tip", "产出地图：万花")
                ))
        ));
    }

    @Test
    void shouldRenderActivityAndFlowerTemplates() throws Exception {
        assertRendered("活动日历", Map.of(
                "server", "乾坤一掷",
                "data", Map.ofEntries(
                        Map.entry("date", "2025-12-03"), Map.entry("week", "三"),
                        Map.entry("war", "大战！英雄冰川宫宝库"), Map.entry("battle", "浮香丘"),
                        Map.entry("orecar", "跨服·河西瀚漠"), Map.entry("school", "明教·漫漫朝圣路"),
                        Map.entry("rescue", "七秀·乱世"), Map.entry("draws", List.of("苍云铁麟·成男", "长歌儒风·成男", "霸刀名少·成男", "蓬莱仙梧·成男", "凌雪冥夜·成男")),
                        Map.entry("leaders", List.of("九辩馆·章危")),
                        Map.entry("luck", List.of("丰丰", "童心客", "沅沅")),
                        Map.entry("cards", List.of("英雄天子峰", "英雄日轮山城", "英雄风雨稻香村")),
                        Map.entry("teams", List.of("公共任务：洛阳城·攻打应天门", "公共任务：洛阳·神兵迷踪", "团队秘境：阆风悬城", "团队秘境：武狱黑牢", "团队秘境：西津渡"))
                )
        ));
        assertRendered("活动月历", Map.of(
                "today", Map.of("date", "2025-12-01", "week", "一"),
                "data", List.of(
                        calendarDay("2025-12-03", "三", "英雄不染窟", "雪域关城", "唐门·兄弟之争"),
                        calendarDay("2025-12-04", "四", "英雄冰川宫宝库", "浮香丘", "明教·漫漫朝圣路"),
                        calendarDay("2025-12-05", "五", "英雄会战弓月城", "云湖天池", "万花·三星望月"),
                        calendarDay("2025-12-06", "六", "英雄尘归海", "九宫棋谷", "七秀·忆盈楼")
                )
        ));
        assertRendered("行侠事件", Map.of(
                "name", "楚天社",
                "data", List.of(
                        Map.of("mapName", "晟江", "event", "恶霸出浴", "site", "白菰里", "description", "公共任务：击退恶霸黄七。", "time", "12:56"),
                        Map.of("mapName", "楚州", "event", "水寨来客", "site", "临江渡", "description", "协助当地侠士清理水寨中的来犯之敌。", "time", "15:20"),
                        Map.of("mapName", "百溪", "event", "山路救援", "site", "照影潭", "description", "保护商队安全通过山道并救治受伤村民。", "time", "18:45")
                )
        ));
        assertRendered("家园鲜花", Map.of(
                "server", "乾坤一掷", "name", "绣球花",
                "data", List.of(
                        Map.of("server", "九寨沟·镜海", "flowers", List.of(
                                Map.of("name", "一级绣球花", "color", "红，白，紫", "price", 1.5D, "lines", List.of("6", "25", "18")),
                                Map.of("name", "二级绣球花", "color", "蓝，粉", "price", 3.2D, "lines", List.of("3", "12"))
                        )),
                        Map.of("server", "乾坤一掷", "flowers", List.of(
                                Map.of("name", "一级绣球花", "color", "红，紫", "price", 1.8D, "lines", List.of("2", "17", "31"))
                        ))
                )
        ));
    }

    @Test
    void shouldRenderRoleCardTemplateWithEmbeddedImage() throws Exception {
        assertRendered("角色名片", Map.of(
                "title", "所有名片", "server", "乾坤一掷", "name", "夜温言",
                "data", List.of(
                        Map.ofEntries(
                                Map.entry("zone", "电信区"), Map.entry("server", "乾坤一掷"),
                                Map.entry("roleName", "夜温言@长安城"), Map.entry("showIndex", 2),
                                Map.entry("active", true), Map.entry("cacheTime", "2026-04-15 12:47:43"),
                                Map.entry("imageDataUri", sampleCardDataUri(new Color(48, 76, 84), new Color(174, 128, 86)))
                        ),
                        Map.ofEntries(
                                Map.entry("zone", "电信区"), Map.entry("server", "唯我独尊"),
                                Map.entry("roleName", "山色暮相依"), Map.entry("showIndex", 1),
                                Map.entry("active", false), Map.entry("cacheTime", "2026-04-12 19:25:10"),
                                Map.entry("imageDataUri", sampleCardDataUri(new Color(84, 67, 91), new Color(91, 145, 137)))
                        )
                )
        ));
    }

    @Test
    void shouldRenderNewsFraudAndItemTemplates() throws Exception {
        List<Map<String, Object>> news = List.of(
                Map.of("type", "公告", "title", "夏日版本更新公告", "date", "2026-07-15 07:30:00"),
                Map.of("type", "活动", "title", "江湖盛夏活动开启", "date", "2026-07-14 18:00:00"),
                Map.of("type", "赛事", "title", "名剑大会赛程调整", "date", "2026-07-13 12:00:00")
        );
        assertRendered("新闻资讯", Map.of("title", "新闻资讯", "data", news));
        assertRendered("新闻资讯", "维护公告", Map.of("title", "维护公告", "data", List.of(
                Map.of("type", "维护", "title", "7 月 15 日例行维护公告", "date", "2026-07-15 06:00:00"),
                Map.of("type", "更新", "title", "客户端资源更新说明", "date", "2026-07-12 09:30:00")
        )));

        assertRendered("骗子查询", Map.of(
                "uid", "570790267",
                "data", List.of(
                        Map.of("server", "乾坤一掷", "tieba", "剑网3吧", "title", "公开避雷记录",
                                "text", "请在交易前再次核实角色、账号与历史记录。", "time", "2026-07-10 16:20:00"),
                        Map.of("server", "梦江南", "tieba", "剑网3交易吧", "title", "交易纠纷记录",
                                "text", "双方对物品交付时间存在争议，建议保留完整沟通凭证。", "time", "2026-06-28 21:15:00")
                )
        ));

        assertRendered("搜索物品", Map.of(
                "name", "十五",
                "data", List.of(
                        Map.ofEntries(
                                Map.entry("category", "道具"), Map.entry("subclass", "节日物品"),
                                Map.entry("name", "十五夜观灯"), Map.entry("alias", "观灯"),
                                Map.entry("value", "12000"), Map.entry("description", "节日活动相关物品"),
                                Map.entry("date", "2026-07-15"),
                                Map.entry("imageDataUri", sampleCardDataUri(new Color(38, 84, 78), new Color(210, 164, 83)))
                        ),
                        Map.ofEntries(
                                Map.entry("category", "家具"), Map.entry("subclass", "灯具"),
                                Map.entry("name", "十五夜花灯"), Map.entry("alias", "花灯"),
                                Map.entry("value", "8600"), Map.entry("description", "可放置于家园的节日灯具"),
                                Map.entry("date", "2026-07-14"),
                                Map.entry("imageDataUri", sampleCardDataUri(new Color(73, 60, 91), new Color(101, 157, 143)))
                        )
                )
        ));
    }

    @Test
    void shouldRenderChatRecordsTemplate() throws Exception {
        List<Map<String, Object>> records = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> Map.<String, Object>of(
                        "zone", "电信区",
                        "server", "乾坤一掷",
                        "roleName", "琉枫",
                        "channel", "世界",
                        "message", "[跨服房间招募·25人普通会战弓月城]【千机】大小M 提升速 来TND TN补500",
                        "time", "2025-12-30 22:58:32"))
                .toList();
        assertRendered("角色聊天", Map.of(
                "server", "乾坤一掷",
                "roleName", "琉枫",
                "page", 1,
                "total", 36,
                "data", records
        ));

        BufferedImage image = ImageIO.read(
                Path.of("target", "test-output", "html-image", "角色聊天.png").toFile());
        assertNotNull(image);
        assertTrue(image.getHeight() > 1800);
    }
    @Test
    void shouldRenderMarketAndUtilityTemplates() throws Exception {
        assertRendered("贴吧物价", Map.of(
                "server", "乾坤一掷", "name", "狐金",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "乾坤一掷", "name", "狐金",
                                "context", "近期成交价稳定，少量现货可议。", "reply", 18, "floor", 6,
                                "time", "2026-07-15 15:00:00"),
                        Map.of("zone", "双线区", "server", "梦江南", "name", "狐金",
                                "context", "收购需求增加，价格仅供当日参考。", "reply", 9, "floor", 12,
                                "time", "2026-07-14 21:30:00")
                )
        ));
        assertRendered("金币价格", Map.of(
                "server", "乾坤一掷",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "乾坤一掷", "tieba", "0.52",
                                "wanbaolou", "0.55", "dd373", "0.53", "date", "2026-07-15"),
                        Map.of("zone", "双线区", "server", "梦江南", "tieba", "0.49",
                                "wanbaolou", "0.51", "dd373", "0.50", "date", "2026-07-14")
                )
        ));
        assertRendered("副本解密", Map.of("data", Map.of(
                "current", Map.of("node", "乾位", "result", "先点亮左侧机关，再等待石灯变色。"),
                "next", Map.of("node", "坎位", "result", "触发中央石灯后按顺序关闭外圈机关。"),
                "time", "2026-07-15 15:00:00", "condition", "完成当前机关后进入下一阶段"
        )));
        assertRendered("统战歪歪", Map.of(
                "server", "乾坤一掷",
                "data", List.of(
                        Map.of("server", "乾坤一掷", "channels", List.of(
                                Map.of("name", "浩气盟统战", "campName", "浩气盟", "users", 186, "limit", 500),
                                Map.of("name", "恶人谷统战", "campName", "恶人谷", "users", 142, "limit", 500)
                        )),
                        Map.of("server", "梦江南", "channels", List.of(
                                Map.of("name", "阵营指挥频道", "campName", "中立", "users", 76, "limit", 300)
                        ))
                )
        ));
    }

    @Test
    void shouldRenderLookupTemplates() throws Exception {
        assertRendered("科举答题", Map.of(
                "subject", "古琴有几根弦",
                "data", List.of(
                        Map.of("question", "古琴有几根弦？", "answer", "七根"),
                        Map.of("question", "《高山流水》常用哪种乐器演奏？", "answer", "古琴"),
                        Map.of("question", "琴弦中象征文武二王的是哪两根？", "answer", "第六弦与第七弦")
                )
        ));
        assertRendered("搜索区服", Map.of(
                "query", "长安城",
                "data", Map.of(
                        "zone", "电信区", "name", "长安城", "center", "唯我独尊",
                        "aliases", List.of("长安", "电五长安"),
                        "slaves", List.of("乾坤一掷", "斗转星移", "剑胆琴心", "唯我独尊")
                )
        ));
        assertRendered("扶摇预测", Map.of(
                "server", "乾坤一掷",
                "data", List.of(
                        Map.of("zone", "电信区", "server", "乾坤一掷", "time", "2025-12-01 15:37:31"),
                        Map.of("zone", "电信区", "server", "唯我独尊", "time", "2025-12-01 16:10:00"),
                        Map.of("zone", "双线区", "server", "梦江南", "time", "2025-12-01 17:25:00")
                )
        ));
    }
    @Test
    void shouldRenderTradeRecordPreviewWithGroupedJx3ApiData() throws Exception {
        Path output = Path.of("target", "test-output", "html-image", "物品价格-金发因陀罗.png");
        Files.createDirectories(output.getParent());

        Map<String, Object> data = Map.ofEntries(
                Map.entry("mode", "物品价格"),
                Map.entry("server", "乾坤一掷"),
                Map.entry("name", "金发·因陀罗"),
                Map.entry("previewImageDataUri", sampleCardDataUri(new Color(245, 239, 225), new Color(218, 183, 86))),
                Map.entry("data", Map.ofEntries(
                        Map.entry("category", "发型"),
                        Map.entry("name", "金发·因陀罗"),
                        Map.entry("alias", "猴金/金发因陀罗"),
                        Map.entry("retail", 280),
                        Map.entry("desc", "2016/02/29上架发售，不绑定限时3周。售价280。"),
                        Map.entry("date", "2016-02-29"),
                        Map.entry("view", sampleCardDataUri(new Color(245, 239, 225), new Color(218, 183, 86))),
                        Map.entry("groups", List.of(
                                priceGroup("公示期", List.of(
                                        priceRow("2026-08-12", "乾坤一掷", 6099, 7),
                                        priceRow("2026-08-12", "梦江南", 6099, 7),
                                        priceRow("2026-08-12", "梦江南", 6186, 7),
                                        priceRow("2026-08-12", "龙争虎斗", 6666, 7)
                                )),
                                priceGroup("在售期", List.of(
                                        priceRow("2026-08-12", "绝代天骄", 6188, 3),
                                        priceRow("2026-08-12", "梦江南", 6333, 3),
                                        priceRow("2026-08-12", "长安城", 6666, 3),
                                        priceRow("2026-08-12", "天鹅坪", 7000, 3),
                                        priceRow("2026-08-12", "龙争虎斗", 30000, 3)
                                )),
                                priceGroup("乾坤一掷", List.of(
                                        priceRow("2026-07-08", "乾坤一掷", 4300, 3),
                                        priceRow("2026-04-15", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-14", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-13", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-12", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-11", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-10", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-09", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-08", "乾坤一掷", 5500, 4),
                                        priceRow("2026-04-07", "乾坤一掷", 5500, 4)
                                )),
                                priceGroup("电信区", List.of(
                                        priceRow("2026-07-08", "乾坤一掷", 4300, 3),
                                        priceRow("2026-07-08", "斗转星移", 4300, 3),
                                        priceRow("2026-07-08", "幽月轮", 4300, 3),
                                        priceRow("2026-06-05", "龙争虎斗", 5000, 4),
                                        priceRow("2026-06-01", "蝶恋花", 5000, 4)
                                )),
                                priceGroup("双线区", List.of(
                                        priceRow("2026-07-08", "破阵子", 4300, 3),
                                        priceRow("2026-04-02", "破阵子", 5500, 4),
                                        priceRow("2026-04-01", "飞龙在天", 5500, 4),
                                        priceRow("2026-02-05", "天鹅坪", 6300, 3)
                                )),
                                priceGroup("无界区", List.of())
                        ))
                ))
        );

        HtmlToImageUtl.renderTemplateToImage("物品价格", data, output.toString());

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 1000);
        assertTrue(image.getHeight() > 1200);
    }

    @Test
    void shouldRenderTradeRecordWhenRemotePreviewCannotBeEmbedded() throws Exception {
        Path output = Path.of("target", "test-output", "html-image", "物品价格-预览图降级.png");
        Files.createDirectories(output.getParent());
        Map<String, Object> data = Map.of(
                "mode", "物品价格",
                "server", "乾坤一掷",
                "name", "金发·因陀罗",
                "data", Map.of(
                        "name", "金发·因陀罗",
                        "view", "https://static.nicemoe.cn/static/view/unavailable.png",
                        "groups", List.of()));

        HtmlToImageUtl.renderTemplateToImage("物品价格", data, output.toString());

        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 1000);
        assertTrue(image.getHeight() > 700);
    }

    @Test
    void shouldReportBrokenImageResourceWithoutDumpingDataUri() throws Exception {
        Path output = Path.of("target", "test-output", "html-image", "坏图片资源.png");
        Files.createDirectories(output.getParent());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> HtmlToImageUtl.renderHtmlContentToImage(
                        "<html><body><img src=\"data:image/png;base64,broken\"></body></html>",
                        output.toString()));

        assertTrue(exception.getMessage().contains("resources=>"));
        assertTrue(exception.getMessage().contains("data:image/png;base64,..."));
        assertFalse(exception.getMessage().contains("base64,broken"));
    }
    private String sampleCardDataUri(Color background, Color accent) throws Exception {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(accent);
            graphics.fillRect(52, 54, 16, 432);
            graphics.fillOval(610, 70, 260, 260);
            graphics.setColor(new Color(235, 239, 236));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 42));
            graphics.drawString("JX3 CHARACTER CARD", 105, 155);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 25));
            graphics.drawString("Validated and embedded image", 108, 205);
            graphics.drawString("No external browser request", 108, 245);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private Map<String, Object> calendarDay(String date, String week, String war, String battle, String school) {
        return Map.ofEntries(
                Map.entry("date", date), Map.entry("week", week), Map.entry("war", war),
                Map.entry("battle", battle), Map.entry("orecar", "跨服·河西瀚漠"),
                Map.entry("school", school), Map.entry("rescue", "少林·乱世"),
                Map.entry("luck", List.of("丰丰", "童心客")),
                Map.entry("cards", List.of("英雄迷渊岛", "尘归海·饕餮洞"))
        );
    }
    private Map<String, Object> priceGroup(String name, List<Map<String, Object>> rows) {
        return Map.of("name", name, "list", rows);
    }

    private Map<String, Object> priceRow(String date, String server, int value, int sale) {
        return Map.of("date", date, "server", server, "value", value, "sale", sale);
    }

    private Map<String, Object> skillData(String name, String summary, String description,
                                          String specialDescription, String interval, String consumption,
                                          String distance, String kind, String subKind,
                                          String releaseType, String weapon) {
        Map<String, Object> skill = new LinkedHashMap<>();
        skill.put("name", name);
        skill.put("summary", summary);
        skill.put("description", description);
        skill.put("specialDescription", specialDescription);
        skill.put("interval", interval);
        skill.put("consumption", consumption);
        skill.put("distance", distance);
        skill.put("kind", kind);
        skill.put("subKind", subKind);
        skill.put("releaseType", releaseType);
        skill.put("weapon", weapon);
        return skill;
    }

    @Test
    void shouldOnlyRetryTransientBrowserTermination() {
        assertTrue(HtmlToImageUtl.isTransientBrowserTermination(
                new com.microsoft.playwright.PlaywrightException("Page closed")));
        assertTrue(HtmlToImageUtl.isTransientBrowserTermination(
                new com.microsoft.playwright.PlaywrightException(
                        "Target page, context or browser has been closed")));
        assertFalse(HtmlToImageUtl.isTransientBrowserTermination(
                new com.microsoft.playwright.PlaywrightException("Vue template failed")));
    }

    private void assertRendered(String templateName, Map<String, Object> data) throws Exception {
        assertRendered(templateName, templateName, data);
    }

    private void assertRendered(String templateName, String outputName, Map<String, Object> data) throws Exception {
        Path output = Path.of("target", "test-output", "html-image", outputName + ".png");
        Files.createDirectories(output.getParent());

        HtmlToImageUtl.renderTemplateToImage(templateName, data, output.toString());

        assertTrue(Files.exists(output), templateName);
        assertTrue(Files.size(output) > 0, templateName);
        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image, templateName);
        assertTrue(image.getWidth() > 700, templateName);
        assertTrue(image.getHeight() > 100, templateName);
    }

    @Test
    void shouldRenderPushSubscriptionTableWithEnabledAndDisabledWsRows() throws Exception {
        com.grafie.botjava.service.push.PushTaskRegistry registry =
                new com.grafie.botjava.service.push.PushTaskRegistry();
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        int index = 0;
        for (com.grafie.botjava.service.push.PushTaskDefinition task : registry.all()) {
            boolean enabled = index++ % 3 == 0;
            rows.add(Map.of(
                    "source", task.source().getDisplayName(),
                    "sourceCode", task.source().name(),
                    "category", task.category(),
                    "name", task.displayName(),
                    "code", task.code(),
                    "enabled", enabled,
                    "ready", task.producerReady()));
        }
        long enabledCount = rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("enabled")))
                .count();
        long wsCount = registry.all().stream()
                .filter(task -> task.source().isWebSocket())
                .count();
        Map<String, Object> data = Map.of(
                "wsCount", wsCount,
                "enabledCount", enabledCount,
                "disabledCount", rows.size() - enabledCount,
                "rows", rows
        );
        Path output = Path.of("target", "test-output", "html-image", "推送列表.png");
        Files.createDirectories(output.getParent());

        HtmlToImageUtl.renderTemplateToImage("推送列表", data, output.toString());

        BufferedImage image = ImageIO.read(output.toFile());
        assertNotNull(image);
        assertTrue(image.getWidth() > 1000);
        assertTrue(image.getHeight() > 2000);
    }
}
