# 部署指南

## 1. 运行环境

- Java 21。
- Maven 3.9 或使用构建后的可执行 JAR。
- PostgreSQL。
- MinIO 或兼容 S3 的自有文件服务。
- Chromium 及 Playwright 系统依赖。
- 可从公网 HTTPS 访问的 QQ webhook 地址和媒体域名。

HTTP 查询与 JX3API WebSocket 相互独立。只部署群指令时不需要启用 WebSocket。

QQ 消息权限、项目频控及审核发布边界见 [QQ 能力权限、频率与发布政策](QQ_CAPABILITY_POLICY.md)。

## 2. 构建

Linux 服务器首次安装 Playwright Chromium：

```bash
mvn -B exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install --with-deps chromium"
```

执行测试并构建：

```bash
mvn -B test
mvn -B package -DskipTests
```

产物位于 `target/botjava-0.0.1.jar`。

构建 Docker 镜像：

```bash
docker build -t jx3bot:local .
```

Dockerfile 使用 Ubuntu/Jammy 系 Java 21 镜像，构建阶段执行 Maven package，运行阶段预装 Playwright Chromium、系统依赖和中文字体，支持 HTML/Vue 截图生成图片。容器运行时仍需通过环境变量注入 QQ、JX3API、PostgreSQL 和 MinIO 等配置：

```bash
docker run --rm \
  --env-file .env \
  -p 8081:8081 \
  -p 8082:8082 \
  jx3bot:local
```

## 3. 必需配置

生产环境不得把凭证写入 `application*.yml`。使用环境变量或部署平台的 Secret 管理功能。

### QQ 机器人

```text
TX_BOT_TOKEN
TX_BOT_APP_SECRET
TX_BOT_APP_ID
TX_BOT_NUMBER
```

QQ OpenAPI 和 access token 地址可以继续使用配置文件中的官方默认值，也可以按环境覆盖。

在发送任何群消息前，可以先执行无副作用的身份鉴权 smoke test。该测试只调用官方 `GET /users/@me`，输出不包含机器人 ID、名称或凭证，普通 Maven 和 CI 不会自动运行：

```powershell
$env:QQ_OPENAPI_LIVE_SMOKE = "true"
$env:TX_BOT_APP_ID = "<app-id>"
$env:TX_BOT_APP_SECRET = "<secret>"
mvn "-Dtest=QqOpenApiLiveSmokeIT" test
```

群消息发送、文件上传、事件回复和主动消息会产生真实平台副作用，不包含在身份 smoke test 中，必须继续按本文发布检查及 `QQ_CAPABILITY_POLICY.md` 在指定测试群人工触发验收。

需要验证真实群发送时，使用单模式 runner。它要求测试群 openid、强确认短语和一种 `QQ_GROUP_LIVE_MODE`，每次最多发送一条最终群消息；普通 Maven 和 CI 不执行：

```powershell
$env:QQ_GROUP_LIVE_SMOKE = "true"
$env:QQ_GROUP_LIVE_CONFIRM = "SEND_TO_TEST_GROUP"
$env:QQ_GROUP_LIVE_MODE = "REPLY"
$env:QQ_GROUP_LIVE_OPENID = "<test-group-openid>"
$env:QQ_GROUP_LIVE_MSG_ID = "<fresh-source-message-id>"
$env:TX_BOT_APP_ID = "<app-id>"
$env:TX_BOT_APP_SECRET = "<secret>"
mvn "-Dtest=QqGroupMessageLiveSmokeIT" test
```

可选模式及额外变量：

| 模式 | 行为 | 必需额外变量 |
| --- | --- | --- |
| `REPLY` | `msg_id + msg_seq=1` 文本被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `REFERENCE` | 引用来源消息的文本被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `EVENT` | `event_id` 事件被动回复 | `QQ_GROUP_LIVE_EVENT_ID` |
| `ACTIVE` | 不携带被动字段的主动文本消息 | 无；仅验证 OpenAPI 传输，应用数据库授权与频控另行验收 |
| `IMAGE` | 上传自有 HTTPS 图片并作为被动媒体回复 | `QQ_GROUP_LIVE_MSG_ID`、`QQ_GROUP_LIVE_IMAGE_URL` |
| `AUDIO` | 上传 HTTPS 音频并作为被动语音回复 | `QQ_GROUP_LIVE_MSG_ID`、`QQ_GROUP_LIVE_AUDIO_URL` |
| `MARKDOWN` | Markdown + 自定义 Keyboard 被动回复 | `QQ_GROUP_LIVE_MSG_ID` |
| `ARK` | Ark 模板 23 被动回复 | `QQ_GROUP_LIVE_MSG_ID` |

`msg_id` 和 `event_id` 必须来自目标测试群的近期真实事件。`IMAGE` 与 `AUDIO` 的 URL 必须是 QQ 可公网拉取的 HTTPS 地址。一次只运行一种模式；切换模式前重新确认平台频率限制。runner 不输出 group openid、消息/事件 ID、请求体或平台响应正文。

### JX3API

项目启用 JX3API HTTP 功能时至少需要：

```text
JX3API_API_TOKEN
JX3API_TICKET
JX3API_DEFAULT_SERVER
```

实际变量名需要与部署环境映射到 `jx3api.api.*`。其中 `ticket` 仅用于需要推栏数据的接口。

#### 在线 HTTP smoke test

普通 `mvn test` 只执行离线 contract，不访问 JX3API。需要用目标环境凭证验收官方 HTTP 接口时，额外提供一个真实、可查询的服务器、角色和用于骗子查询的 QQ 号，然后显式执行：

```powershell
$env:JX3API_LIVE_SMOKE = "true"
$env:JX3API_API_TOKEN = "<secret>"
$env:JX3API_TICKET = "<secret>"
$env:JX3API_SMOKE_SERVER = "<server>"
$env:JX3API_SMOKE_ROLE = "<role>"
$env:JX3API_SMOKE_UID = "<qq-number>"
$env:JX3API_SMOKE_DELAY_MS = "500"
mvn "-Dtest=Jx3ApiLiveSmokeIT" test
```

runner 从机器可读覆盖矩阵生成 60 个非语音官方接口命令，复用真实 `REGEX -> Action -> Jx3RequestUtil -> DTO` 链路；阿里语音按独立验收项执行。输出只包含 `MethodEnum`、固定接口路径、`SUCCESS/EMPTY/FAILED` 和官方 code，不输出命令、请求参数或响应数据。`EMPTY` 表示接口成功但当前条件下无数据，不等价于 DTO 在线反序列化成功；更换有效查询条件后应再次验证。测试不会由普通 Maven 或 CI 自动执行。

### 指令发布模式

生产环境必须显式配置：

```text
BOT_COMMAND_RUNTIME_MODE=PRODUCTION
```

可选值为 `PRODUCTION`、`TEST`、`REVIEW`。`TEST` 会允许已开启群实验开关的群使用实验指令；`REVIEW` 仅用于部署审核分支和审核专用指令，不应作为正式环境配置。

### PostgreSQL

```text
DB_USERNAME
DB_PASSWORD
```

默认连接地址为 `jdbc:postgresql://localhost:5432/postgres?currentSchema=public`。生产环境应通过 Spring 配置覆盖完整 JDBC URL。

开发环境的 `spring.jpa.hibernate.ddl-auto=update` 会自动维护表结构。生产环境使用 `ddl-auto=none` 时，在发布个人多角色绑定和指令调用记录功能前执行；`user-info.sql` 会创建角色集合表，并幂等迁移旧的默认角色：

```bash
psql "$DATABASE_URL" -f docs/database/group-info.sql
psql "$DATABASE_URL" -f docs/database/user-info.sql
psql "$DATABASE_URL" -f docs/database/group-command-setting.sql
psql "$DATABASE_URL" -f docs/database/group-command-cooldown.sql
psql "$DATABASE_URL" -f docs/database/jx3-api-cache.sql
psql "$DATABASE_URL" -f docs/database/command-invocation.sql
```

`user-info.sql` 会为旧 `user_info` 表幂等增加 `school` 字段，并保留现有默认角色及多角色数据。

`group-info.sql` 会增加主动消息本地开关、QQ 平台授权和共享频控字段，并为 `open_group_id` 建立唯一索引。旧库执行脚本前先检查重复群记录；若查询有结果，应先按业务数据合并重复行：

```sql
SELECT open_group_id, COUNT(*)
FROM group_info
WHERE open_group_id IS NOT NULL
GROUP BY open_group_id
HAVING COUNT(*) > 1;
```

群主动消息默认关闭，必须同时满足两项条件：群主或管理员执行 `群设置 主动消息 开启`，并且 QQ 平台向 webhook 投递 `GROUP_MSG_RECEIVE`。`GROUP_MSG_REJECT`、`GROUP_DEL_ROBOT` 或重新加入群会把平台授权置为关闭，但不会改写管理员的本地开关。默认每群 60 秒最多一条主动消息，频控占位保存在数据库中，多应用实例共享；可通过以下环境变量调整：

```text
BOT_QQ_ACTIVE_MESSAGE_MIN_INTERVAL_SECONDS=60
```

满足上述条件后，群主或管理员可使用 `群公告 内容` 发布主动消息。发送被平台或本地策略拒绝时，机器人会通过来源消息被动回复具体原因；公告 Action 不直接调用 QQ 接口。

群指令调用记录默认保留 30 天，并在每天 `03:15` 清理。可通过以下环境变量调整：

```text
BOT_COMMAND_AUDIT_RETENTION_DAYS=30
BOT_COMMAND_AUDIT_CLEANUP_CRON=0 15 3 * * *
BOT_COMMAND_STATISTICS_DEFAULT_DAYS=7
BOT_COMMAND_STATISTICS_MAX_DAYS=90
```

群指令冷却状态保存在 `group_command_cooldown`，因此多应用实例共享同一冷却窗口。Action 或 QQ 发送尚未结束时使用执行中租约防止重复处理；实例异常退出后默认 300 秒可恢复，允许通过以下环境变量在 30 至 3600 秒范围内调整：

```text
BOT_COMMAND_COOLDOWN_IN_FLIGHT_TIMEOUT_SECONDS=300
```

明确配置缓存策略的 JX3API 公共查询会写入 `jx3_api_cache`，多应用实例共享同一 TTL 缓存。token 不进入缓存键，失败响应、实时角色查询和超过 1 MB 的响应不会缓存；过期记录默认每天 `03:45` 清理：

```text
JX3API_CACHE_CLEANUP_CRON=0 45 3 * * *
```

群主或管理员可使用 `群统计` 或 `群统计 30` 查看当前群的聚合结果；统计不返回成员标识、消息原文或请求参数。

`command_invocation` 不保存原始聊天内容和请求参数。生产环境应按业务量制定数据保留和归档周期，避免调用记录无限增长。

#### PostgreSQL 双实例验收

上线多实例前，应在目标 PostgreSQL 中创建一个空的专用 schema。schema 名必须以 `botjava_smoke_` 开头，禁止使用 `public` 或生产业务 schema：

```sql
CREATE SCHEMA botjava_smoke_acceptance AUTHORIZATION botjava_smoke_user;
```

显式设置下列变量后运行双实例验收。JDBC URL 的 `currentSchema` 必须与 `POSTGRES_LIVE_SCHEMA` 完全一致；普通 `mvn test` 和 CI 不会连接该数据库：

```powershell
$env:POSTGRES_LIVE_SMOKE = "true"
$env:POSTGRES_LIVE_CONFIRM = "USE_DEDICATED_SMOKE_SCHEMA"
$env:POSTGRES_LIVE_JDBC_URL = "jdbc:postgresql://db.example.com:5432/botjava?sslmode=require&currentSchema=botjava_smoke_acceptance"
$env:POSTGRES_LIVE_USERNAME = "botjava_smoke_user"
$env:POSTGRES_LIVE_PASSWORD = "<password>"
$env:POSTGRES_LIVE_SCHEMA = "botjava_smoke_acceptance"
mvn "-Dtest=PostgresTwoInstanceLiveSmokeIT" test
```

runner 会启动两个相互独立的 Spring/JPA 上下文，并验证群绑定、个人绑定、指令冷却竞争及过期租约接管、主动消息频控竞争、JX3API 共享缓存和调用审计跨实例可见。执行前它要求所有业务表均为空；空库检查失败时不会执行任何清理，通过后才会在结束阶段删除本次 schema 中的业务行，并保留 schema 和 Hibernate 创建的表。该账号只能获得专用 smoke schema 权限，不得授予生产 schema 的写权限。

### 运行指标

Actuator 默认在本机 `127.0.0.1:8082` 提供健康检查和 Prometheus 指标，不经过 QQ webhook 的业务端口：

```text
GET http://127.0.0.1:8082/actuator/health
GET http://127.0.0.1:8082/actuator/prometheus
```

自定义监听地址和端口：

```text
MANAGEMENT_SERVER_ADDRESS=127.0.0.1
MANAGEMENT_SERVER_PORT=8082
```

QQ token 与 OpenAPI 请求默认 10 秒超时，可设置为 1 至 60 秒：

```text
TX_BOT_REQUEST_TIMEOUT_SECONDS=10
```

默认的 `/actuator/health` 不访问外部 QQ 服务。需要同时验证 QQ 鉴权和连通性时，可显式开启官方 `GET /users/@me` 探测：

```text
TX_BOT_HEALTH_CHECK_ENABLED=true
```

启用后，QQ 认证失败、限流、超时或网络故障会使整体健康状态变为 `DOWN`。健康详情保持隐藏，探测结果不包含机器人 ID、用户名、token 或上游响应；部署平台配置自动重启前，应先确认是否希望把 QQ 的短暂波动视为应用故障。

核心业务指标包括 `bot_command_duration_*`、`bot_jx3_request_duration_*`、`bot_jx3_cache_requests_total` 和 `bot_qq_request_duration_*`。标签只包含指令、固定接口路径、操作及结果类型，不包含群 openid、成员 openid、消息内容或角色名。

数据库共享缓存的 5 分钟命中率可以使用以下 PromQL；只有明确配置缓存策略的接口才计入 hit/miss：

```promql
sum(rate(bot_jx3_cache_requests_total{result="hit"}[5m]))
/
sum(rate(bot_jx3_cache_requests_total[5m]))
```

Prometheus 与应用不在同一主机时，可以把管理地址改为内网地址，并通过防火墙或反向代理鉴权限制来源；不要直接把 Actuator 管理端口暴露到公网。

目标环境可显式执行 Prometheus 指标 smoke test。该测试会读取 `OBSERVABILITY_PROMETHEUS_URL` 指向的文本端点，验证四类 `bot_*` 指标已被暴露，并确认响应文本中没有群 openid、成员 openid、消息 ID、角色名或消息内容等标签；输出只包含 host 和 HTTP 状态，不打印完整 URL 或响应正文：

```powershell
$env:OBSERVABILITY_LIVE_SMOKE = "true"
$env:OBSERVABILITY_PROMETHEUS_URL = "http://127.0.0.1:8082/actuator/prometheus"
mvn "-Dtest=ObservabilityLiveSmokeIT" test
```

### MinIO

```text
MINIO_ENDPOINT
MINIO_PUBLIC_URL
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
MINIO_BUCKET
```

`MINIO_ENDPOINT` 是应用连接对象存储使用的地址，可以是内网 HTTP；`MINIO_PUBLIC_URL` 是拼装媒体返回值的公网 HTTPS 基础域名，两者不得混为一谈。图片模板会先在 `target/generated-images/html` 生成 PNG，再上传 MinIO。MinIO bucket 或反向代理必须允许 QQ 平台通过公网 HTTPS 匿名读取生成的媒体 URL。

建议为 bucket 配置独立域名，例如 `media.example.com`，由 Nginx 或 Caddy 反向代理 MinIO。业务代码只依赖最终可公开访问的 URL，不依赖具体文件服务器产品。

配置完成后，可显式执行媒体域名端到端验收。runner 会使用现有 Vue 模板生成真实 PNG，以唯一对象名上传，再通过 `MINIO_PUBLIC_URL` 匿名回读并校验 HTTP 200、`image/png`、5 MB 上限和 PNG 签名；不会覆盖或删除既有对象：

```powershell
$env:MINIO_LIVE_SMOKE = "true"
$env:MINIO_LIVE_CONFIRM = "UPLOAD_AND_FETCH_TEST_IMAGE"
$env:MINIO_ENDPOINT = "http://minio.internal:9000"
$env:MINIO_PUBLIC_URL = "https://media.example.com"
$env:MINIO_ACCESS_KEY = "<access-key>"
$env:MINIO_SECRET_KEY = "<secret-key>"
$env:MINIO_BUCKET = "qbot"
mvn "-Dtest=MinioMediaDomainLiveSmokeIT" test
```

成功后输出的 `QQ_GROUP_LIVE_IMAGE_URL` 可直接交给 QQ 群发送 runner 的 `IMAGE` 模式，完成 QQ 平台拉取验收。测试对象会保留在 bucket 中作为证据，后续由对象生命周期策略统一清理。

### 阿里语音

语音功能默认关闭。启用时配置：

```text
JX3_SOUND_ENABLED=true
JX3_SOUND_APPKEY
JX3_SOUND_ACCESS
JX3_SOUND_SECRET
JX3_SOUND_VOICE=Aitong
JX3_SOUND_FORMAT=mp3
JX3_SOUND_SAMPLE_RATE=16000
JX3_SOUND_VOLUME=50
JX3_SOUND_SPEECH_RATE=0
JX3_SOUND_PITCH_RATE=0
```

配置完成后可以显式执行阿里语音生成 smoke test。该测试会请求真实 JX3API 语音转换接口，并验证返回的是 HTTPS 音频地址；输出只包含音频 host，不打印完整音频 URL、appkey、access 或 secret：

```powershell
$env:JX3_SOUND_LIVE_SMOKE = "true"
$env:JX3_SOUND_ENABLED = "true"
$env:JX3API_API_TOKEN = "<secret>"
$env:JX3_SOUND_APPKEY = "<appkey>"
$env:JX3_SOUND_ACCESS = "<access>"
$env:JX3_SOUND_SECRET = "<secret>"
$env:JX3_SOUND_LIVE_TEXT = "botjava-jx3 sound live smoke"
mvn "-Dtest=SoundConverterLiveSmokeIT" test
```

此外，目标群必须开启实验功能。生成的音频 URL 会通过 QQ 群文件接口按语音类型上传，再作为 media 消息发送。真实平台验收时，将生成出的 HTTPS MP3 URL 填入 `QQ_GROUP_LIVE_AUDIO_URL`，并使用 `QQ_GROUP_LIVE_MODE=AUDIO` 执行 `QqGroupMessageLiveSmokeIT`。

## 4. 启动

```bash
java -jar target/botjava-0.0.1.jar \
  --spring.profiles.active=prod
```

建议使用 systemd、Docker 或其他进程管理器，并把日志目录挂载到持久化磁盘。

## 5. QQ 回调

- 应用监听端口以 Spring `server.port` 为准。
- webhook 路径为 `/bot/message`。
- 公网入口必须使用 HTTPS，并转发原始请求体和必要请求头。
- `GROUP_AT_MESSAGE_CREATE` 与 `GROUP_MESSAGE_CREATE` 使用同一群消息处理链路。

本地或预发布环境可以使用：

```powershell
./tools/replay-payload.ps1 -Url http://localhost:8081/bot/message
```

回放样例位于 `docs/testing/payloads/group-message-create.json`。

## 6. 发布检查

1. `./tools/verify-utf8.ps1`、`git diff --check` 和 `mvn test` 全部通过。
2. Playwright 能生成非空 PNG。
3. MinIO 上传后的 URL 可从公网匿名读取。
4. QQ webhook 校验成功。
5. 群内文本和图片各完成一次真实发送。
6. 启用语音时，额外验证 MP3 URL 和 QQ 群语音发送。
7. 日志中不得出现 token、ticket、appkey、secret 或数据库密码。
8. 确认 `BOT_COMMAND_RUNTIME_MODE` 与部署环境一致，正式环境必须为 `PRODUCTION`。
9. 检查 `/actuator/health`，并确认 Prometheus 能采集三个 `bot_*_duration` 指标族。
10. 运行外部验收前，可先用 `./tools/check-external-acceptance-env.ps1 -Scope all` 做环境变量预检查；脚本只输出检查结果，不打印变量值。具备真实凭证和确认变量后，可用 `./tools/invoke-external-acceptance.ps1 -Scope <scope>` 统一执行对应 live smoke；不确定时先加 `-DryRun` 查看将执行的测试类。
11. 正式部署前，可用 `./tools/check-external-acceptance-env.ps1 -Scope production` 检查 QQ、JX3API、数据库和媒体域名的必需生产变量是否齐备；该检查同样不打印变量值。
12. 外部验收完成后，可先用 `./tools/new-external-acceptance-record.ps1` 生成脱敏记录草稿，再按 `docs/testing/external-acceptance-record-template.md` 整理结果，并追加到 `docs/PROJECT_DESIGN.md`。

CI 使用 `.github/workflows/ci.yml`，在 Ubuntu 22.04、Java 21 环境对 Java、Markdown、HTML、JSON、YAML、SQL、PowerShell、XML 和 Properties 文件执行 UTF-8 检查，校验本次 push 或 PR 变更中的 Git 空白错误，安装 Chromium 后运行完整测试。
