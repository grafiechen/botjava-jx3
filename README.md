# botjava-jx3

基于腾讯机器人开放平台和 JX3API 的剑网 3 群聊机器人。

当前项目的详细设计、Feature 清单、模块职责和后续规划统一维护在设计文档中：

- [项目设计文档](docs/PROJECT_DESIGN.md)

## 项目文档

- [项目设计文档](docs/PROJECT_DESIGN.md)
- [腾讯机器人开放平台文档](https://bot.q.qq.com/wiki/develop/api/)
- [腾讯机器人 OpenAPI v2 文档](https://bot.q.qq.com/wiki/develop/api-v2/)
- [JX3API 文档](https://www.jx3api.com/#/doc/)
- [JX3API Java SDK](https://github.com/JX3API/jx3api-java/)
- [Playwright for Java](https://playwright.dev/java/)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)

## 技术栈

- Java 21
- Spring Boot 3
- Jackson
- Spring Web / WebFlux / WebSocket
- JPA
- PostgreSQL
- Playwright
- MinIO
- Lombok

## 本地运行

配置文件：

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`

常见配置项：

- `tx.bot.openapi-url`
- `tx.bot.access-token-url`
- `tx.bot.app-id`
- `tx.bot.app-secret`
- `jx3.api.ticket`
- `jx3.api.api-token`
- `jx3.api.default-server`
- `minio.endpoint`
- `minio.bucket-name`

编译：

```bash
mvn -DskipTests compile
```

启动：

```bash
mvn spring-boot:run
```

## AI 编程注意事项

这个区块用于给 AI 编程助手提供项目约束。后续可以继续补充。

### 基础原则

- 所有代码、Markdown、HTML 模板必须使用 UTF-8。
- 修改中文内容后要检查是否乱码。
- 不要把临时审核返回、mock 文案、写死样例写进长期设计文档。
- 修改代码前先读现有模式，优先复用已有 action、DTO、util。
- 不要随意重构无关模块。
- 不要删除用户已有改动。
- 新增 JX3 指令时，优先沿用 `REGEX -> Jx3BaseAction -> MethodEnum -> DTO` 链路。
- 子类必须显式实现 `dealAfterJx3ApiRequest`。
- 默认文本返回；只有需要截图时才 override `getResponseType()` 为 `IMAGE`。
- Vue 模板负责页面结构和渲染，Java 只负责准备 `window.__BOT_DATA__` 数据。

## 当前说明

README 只作为开发入口和索引文件。项目设计、功能清单、模块职责、新增指令流程、图片消息链路等详细内容以 [项目设计文档](docs/PROJECT_DESIGN.md) 为准。
