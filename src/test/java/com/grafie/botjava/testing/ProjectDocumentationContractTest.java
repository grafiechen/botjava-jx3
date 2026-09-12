package com.grafie.botjava.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDocumentationContractTest {

    private static final Path README = Path.of("README.md");
    private static final Path ARCHITECTURE = Path.of("docs", "PROJECT_ARCHITECTURE.md");
    private static final Path DEPLOYMENT = Path.of("docs", "DEPLOYMENT.md");
    private static final Path MONGODB = Path.of("docs", "MONGODB.md");
    private static final Path USER_INFO_SQL = Path.of("docs", "database", "user-info.sql");
    private static final Path ACCEPTANCE_TEMPLATE = Path.of(
            "docs", "testing", "external-acceptance-record-template.md");
    private static final Path ACCEPTANCE_PREFLIGHT_SCRIPT = Path.of(
            "tools", "check-external-acceptance-env.ps1");
    private static final Path ACCEPTANCE_RUNNER_SCRIPT = Path.of(
            "tools", "invoke-external-acceptance.ps1");
    private static final Path ACCEPTANCE_RECORD_SCRIPT = Path.of(
            "tools", "new-external-acceptance-record.ps1");
    private static final Path HTTP_CHECK = Path.of("docs", "testing", "jx3api-http-check.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldKeepReadmeAsProjectIndex() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("[项目设计文档](docs/PROJECT_ARCHITECTURE.md)"));
        assertTrue(readme.contains("[变更历史](docs/PROJECT_DESIGN.md)"));
        assertTrue(readme.contains("## AI 编程注意事项"));
        assertFalse(readme.contains("## 技术栈"));
        assertFalse(readme.contains("## 本地运行"));
        assertFalse(readme.contains("mvn spring-boot:run"));
    }

    @Test
    void shouldKeepArchitectureCoverageNumbersAlignedWithCode() throws Exception {
        String architecture = read(ARCHITECTURE);
        Map<String, Object> checkDocument = OBJECT_MAPPER.readValue(
                HTTP_CHECK.toFile(), new TypeReference<>() {
                });
        List<?> contracts = (List<?>) checkDocument.get("httpContracts");
        long imageCommands = Arrays.stream(REGEX.values())
                .filter(this::usesImageResponse)
                .count();

        assertTrue(architecture.contains("当前覆盖 " + contracts.size() + " 个可执行 contract"));
        assertTrue(architecture.contains("当前注册 " + REGEX.values().length + " 条指令"));
        assertTrue(architecture.contains("当前图片响应清单共 " + imageCommands + " 条"));
        assertTrue(architecture.contains("项目使用四个独立口径描述完成度"));
    }

    @Test
    void shouldDistinguishOfflineCoverageFromExternalAcceptance() throws Exception {
        String architecture = read(ARCHITECTURE);

        assertTrue(architecture.contains("### 10.1 外部环境验收门槛"));
        assertTrue(architecture.contains("真实 smoke test 待执行"));
        assertTrue(architecture.contains("平台验收待执行"));
        assertTrue(architecture.contains("语音生成 live runner 与 QQ 群消息 `AUDIO` live smoke 入口已具备"));
        assertTrue(architecture.contains("本地指标、MDC、Prometheus scrape 契约与目标环境 Prometheus live runner 完成"));
        assertTrue(architecture.contains("`水墨圈圈` 与 `吃瓜` 目前只有 `EXPERIMENTAL` 指令入口"));
    }

    @Test
    void shouldDocumentQqWebSocketDefaultIngressAndWebhookFallback() throws Exception {
        String architecture = read(ARCHITECTURE);
        String deployment = read(DEPLOYMENT);
        String application = read(Path.of("src", "main", "resources", "application.yml"));

        assertTrue(application.contains("BOT_QQ_MESSAGE_INGRESS_MODE:WS"));
        assertTrue(application.contains("BOT_QQ_WEBSOCKET_INTENTS:33554432"));
        assertTrue(deployment.contains("QQ 群消息入口默认使用 QQ v2 WebSocket"));
        assertTrue(deployment.contains("BOT_QQ_MESSAGE_INGRESS_MODE=HOOK"));
        assertTrue(architecture.contains("QQ v2 WebSocket 是默认消息入口"));
        assertTrue(architecture.contains("JX3API WebSocket 仍是游戏事件推送基础结构"));
    }

    @Test
    void shouldKeepExternalAcceptanceTemplateLinkedAndRedacted() throws Exception {
        String architecture = read(ARCHITECTURE);
        String deployment = read(DEPLOYMENT);
        String template = read(ACCEPTANCE_TEMPLATE);
        String templatePath = "docs/testing/external-acceptance-record-template.md";

        assertTrue(Files.isRegularFile(ACCEPTANCE_TEMPLATE));
        assertTrue(architecture.contains(templatePath));
        assertTrue(deployment.contains(templatePath));
        assertTrue(template.contains("不要在仓库中保存真实 token、ticket、secret、openid、消息正文"));
        assertTrue(template.contains("JX3API 在线查询"));
        assertTrue(template.contains("QQ webhook 与群消息"));
        assertTrue(template.contains("PostgreSQL 多实例行为"));
        assertTrue(template.contains("GitHub Actions"));
    }

    @Test
    void shouldKeepExternalAcceptancePreflightLinkedAndRedacted() throws Exception {
        String deployment = read(DEPLOYMENT);
        String script = read(ACCEPTANCE_PREFLIGHT_SCRIPT);
        String runner = read(ACCEPTANCE_RUNNER_SCRIPT);
        String scriptPath = "tools/check-external-acceptance-env.ps1";
        String runnerPath = "tools/invoke-external-acceptance.ps1";

        assertTrue(Files.isRegularFile(ACCEPTANCE_PREFLIGHT_SCRIPT));
        assertTrue(Files.isRegularFile(ACCEPTANCE_RUNNER_SCRIPT));
        assertTrue(deployment.contains(scriptPath));
        assertTrue(deployment.contains(runnerPath));
        assertTrue(deployment.contains("脚本只输出检查结果，不打印变量值"));
        assertTrue(deployment.contains("QQ_GROUP_LIVE_MODE=AUDIO"));
        assertTrue(deployment.contains("mvn \"-Dtest=SoundConverterLiveSmokeIT\" test"));
        assertTrue(deployment.contains("mvn \"-Dtest=ObservabilityLiveSmokeIT\" test"));
        assertTrue(deployment.contains("./tools/check-external-acceptance-env.ps1 -Scope production"));
        assertTrue(script.contains("[ValidateSet(\"all\", \"jx3\", \"qq-openapi\", \"qq-group\", \"minio\", \"postgres\", \"sound\", \"observability\", \"production\")]"));
        assertTrue(script.contains("Values were not printed"));
        assertTrue(script.contains("Format-Table Group, Name, Passed, Message"));
        assertTrue(script.contains("\"JX3API_TICKET\""));
        assertTrue(script.contains("\"JX3API_DEFAULT_SERVER\""));
        assertTrue(script.contains("\"DB_USERNAME\""));
        assertTrue(script.contains("\"DB_PASSWORD\""));
        assertTrue(script.contains("\"QQ_GROUP_LIVE_AUDIO_URL\""));
        assertTrue(script.contains("\"JX3_SOUND_LIVE_SMOKE\""));
        assertTrue(script.contains("\"JX3API_API_TOKEN\""));
        assertTrue(script.contains("\"OBSERVABILITY_LIVE_SMOKE\""));
        assertTrue(script.contains("\"OBSERVABILITY_PROMETHEUS_URL\""));
        assertTrue(script.contains("IMAGE, AUDIO, MARKDOWN"));
        assertTrue(runner.contains("[switch]$DryRun"));
        assertTrue(runner.contains("[ValidateSet(\"all\", \"jx3\", \"qq-openapi\", \"qq-group\", \"minio\", \"postgres\", \"sound\", \"observability\", \"production\")]"));
        assertTrue(runner.contains("Jx3ApiLiveSmokeIT"));
        assertTrue(runner.contains("QqOpenApiLiveSmokeIT"));
        assertTrue(runner.contains("QqGroupMessageLiveSmokeIT"));
        assertTrue(runner.contains("MinioMediaDomainLiveSmokeIT"));
        assertTrue(runner.contains("PostgresTwoInstanceLiveSmokeIT"));
        assertTrue(runner.contains("SoundConverterLiveSmokeIT"));
        assertTrue(runner.contains("ObservabilityLiveSmokeIT"));
        assertTrue(runner.contains("Values were not printed"));
    }

    @Test
    void shouldKeepExternalAcceptanceRecordGeneratorLinkedAndRedacted() throws Exception {
        String deployment = read(DEPLOYMENT);
        String script = read(ACCEPTANCE_RECORD_SCRIPT);
        String scriptPath = "tools/new-external-acceptance-record.ps1";

        assertTrue(Files.isRegularFile(ACCEPTANCE_RECORD_SCRIPT));
        assertTrue(deployment.contains(scriptPath));
        assertTrue(script.contains("External acceptance record draft written"));
        assertTrue(script.contains("Values were not printed"));
        assertTrue(script.contains("docs/PROJECT_DESIGN.md"));
        assertFalse(script.contains("GetEnvironmentVariable"));
    }

    @Test
    void shouldDocumentConfigurableDpsExperimentalService() throws Exception {
        String architecture = read(ARCHITECTURE);
        String application = read(Path.of("src", "main", "resources", "application.yml"));
        String dpsAction = read(Path.of("src", "main", "java", "com", "grafie", "botjava",
                "jx3", "http", "action", "DpsComputeAction.java"));

        assertTrue(architecture.contains("DPS 实验指令的 token、服务地址、请求路径和模型名均由 `jx3api.api.dps-*` 配置提供"));
        assertTrue(application.contains("JX3API_DPS_SERVICE_URL"));
        assertTrue(application.contains("JX3API_DPS_SERVICE_PATH"));
        assertTrue(application.contains("JX3API_DPS_MODEL"));
        assertFalse(dpsAction.contains("先写死"));
    }

    @Test
    void shouldDocumentAllRoleMongoFieldQuery() throws Exception {
        String mongodb = read(MONGODB);

        assertTrue(mongodb.contains("查询全部信息 Mongo字段"));
        assertTrue(mongodb.contains("LuaRoleStatusStore.findAllRoleFields"));
        assertTrue(mongodb.contains("src/main/resources/static/查询全部信息.html"));
        assertTrue(mongodb.contains("每行最多 5 个角色"));
        assertTrue(mongodb.contains("不套用单角色 `BOT_MONGODB_QUERY_FIELD_MAX_ITEMS` 限制"));
    }
    @Test
    void shouldMigrateLegacyRoleBindingConstraintToGroupScope() throws Exception {
        String migration = read(USER_INFO_SQL);

        assertTrue(migration.contains("DROP CONSTRAINT IF EXISTS uk_user_role_binding_account_role"));
        assertTrue(migration.contains("ARRAY['member_openid', 'server', 'role_name']::TEXT[]"));
        assertTrue(migration.contains("UNIQUE (group_open_id, member_openid, server, role_name)"));
    }
    private boolean usesImageResponse(REGEX definition) {
        Path source = Path.of("src", "main", "java",
                definition.getBaseAction().getName().replace('.', '/') + ".java");
        try {
            return read(source).contains("ResponseType.IMAGE");
        }
        catch (Exception exception) {
            throw new IllegalStateException("无法读取 Action 源码：" + source, exception);
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
