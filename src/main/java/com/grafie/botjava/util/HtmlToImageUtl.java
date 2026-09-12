package com.grafie.botjava.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * 转换html生成图片
 *
 * @author grafie.chen
 * @since 2025/1/24  17:10
 */
public class HtmlToImageUtl {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TEMPLATE_DIR = "static/";
    private static final String HTML_SUFFIX = ".html";
    private static final int DEFAULT_VIEWPORT_WIDTH = 1280;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 720;
    private static final int MAX_BROWSER_ATTEMPTS = 2;
    private static final Path DEFAULT_OUTPUT_DIR = Paths.get("target", "generated-images", "html");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String CJK_FONT_FAMILY = """
            <style id="bot-cjk-font-fallback">
            :root {
                --bot-cjk-font-family: "Noto Sans CJK SC", "Noto Sans CJK JP", "Noto Sans CJK TC",
                    "Noto Serif CJK SC", "Microsoft YaHei", "PingFang SC", "Noto Sans", sans-serif;
            }
            html, body, #main, #main * {
                font-family: var(--bot-cjk-font-family) !important;
            }
            </style>
            """;

    /**
     * 根据模板名渲染图片。
     * 传入 "角色详情" 时，会自动匹配 resources/static/角色详情.html。
     *
     * @param htmlName HTML 模板名，可以不带 .html 后缀
     * @param data     模板数据，会注入 window.__BOT_DATA__
     * @return 临时 PNG 文件路径
     * @throws Exception 如果操作失败
     */
    public static String renderTemplateToImage(String htmlName, Object data) throws Exception {
        String outputPath = buildDefaultOutputPath(htmlName).toString();
        return renderTemplateToImage(htmlName, data, outputPath);
    }

    /**
     * 根据模板名和 JSON 字符串渲染图片。
     *
     * @param htmlName HTML 模板名，可以不带 .html 后缀
     * @param jsonData JSON 字符串模板数据
     * @return 临时 PNG 文件路径
     * @throws Exception 如果操作失败
     */
    public static String renderTemplateJsonToImage(String htmlName, String jsonData) throws Exception {
        Object data = OBJECT_MAPPER.readValue(jsonData, Object.class);
        return renderTemplateToImage(htmlName, data);
    }

    /**
     * 根据模板名和 JSON 字符串渲染图片。
     *
     * @param htmlName   HTML 模板名，可以不带 .html 后缀
     * @param jsonData   JSON 字符串模板数据
     * @param outputPath 输出图片路径
     * @return 输出图片路径
     * @throws Exception 如果操作失败
     */
    public static String renderTemplateJsonToImage(String htmlName, String jsonData, String outputPath) throws Exception {
        Object data = OBJECT_MAPPER.readValue(jsonData, Object.class);
        return renderTemplateToImage(htmlName, data, outputPath);
    }

    /**
     * 根据模板名渲染图片。
     *
     * @param htmlName   HTML 模板名，可以不带 .html 后缀
     * @param data       模板数据，会注入 window.__BOT_DATA__
     * @param outputPath 输出图片路径
     * @return 输出图片路径
     * @throws Exception 如果操作失败
     */
    public static String renderTemplateToImage(String htmlName, Object data, String outputPath) throws Exception {
        String htmlFilePath = resolveHtmlTemplatePath(htmlName);
        return renderVueTemplateToImage(htmlFilePath, data, outputPath);
    }

    public static Path getDefaultOutputDir() {
        return DEFAULT_OUTPUT_DIR;
    }

    /**
     * 渲染 Vue HTML 模板为图片。Java 将数据注入 window.__BOT_DATA__，模板自行用 Vue 渲染。
     *
     * @param htmlFilePath 资源目录下 HTML 文件路径
     * @param data         模板数据
     * @param outputPath   输出图片路径
     * @return 输出图片路径
     * @throws Exception 如果操作失败
     */
    public static String renderVueTemplateToImage(String htmlFilePath, Object data, String outputPath) throws Exception {
        String htmlContent = readHtmlFromResources(htmlFilePath);
        htmlContent = inlineKnownScripts(htmlContent);
        htmlContent = injectVueData(htmlFilePath, htmlContent, data);
        return renderHtmlContentToImage(htmlContent, outputPath);
    }

    /**
     * 渲染 HTML 内容为图片
     *
     * @param htmlContent HTML 内容
     * @param outputPath  输出图片路径
     * @return 输出图片路径
     * @throws Exception 如果操作失败
     */
    public static String renderHtmlContentToImage(String htmlContent, String outputPath) throws Exception {
        for (int attempt = 1; attempt <= MAX_BROWSER_ATTEMPTS; attempt++) {
            try {
                return renderHtmlContentToImageOnce(htmlContent, outputPath);
            } catch (PlaywrightException exception) {
                if (attempt == MAX_BROWSER_ATTEMPTS || !isTransientBrowserTermination(exception)) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("HTML 图片渲染未执行");
    }

    private static String renderHtmlContentToImageOnce(String htmlContent, String outputPath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.route("**/*", HtmlToImageUtl::routePageResource);

            page.setViewportSize(DEFAULT_VIEWPORT_WIDTH, DEFAULT_VIEWPORT_HEIGHT);
            page.setContent(htmlContent);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            waitForRenderReady(page);
            assertPageResourcesLoaded(page);

            Locator captureRoot = page.locator("[data-capture-root]");
            if (captureRoot.count() > 0) {
                captureRoot.first().screenshot(new Locator.ScreenshotOptions()
                        .setPath(Paths.get(outputPath)));
            } else {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(outputPath))
                        .setFullPage(true));
            }

            return outputPath;
        }
    }

    static boolean isTransientBrowserTermination(PlaywrightException exception) {
        String message = exception == null ? null : exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("page closed")
                || normalized.contains("browser closed")
                || normalized.contains("browser has been closed")
                || normalized.contains("connection closed");
    }

    /**
     * 读取资源目录中的 HTML 文件内容
     *
     * @param filePath 资源目录下的文件路径
     * @return 文件内容
     * @throws Exception 如果文件读取失败
     */
    public static String readHtmlFromResources(String filePath) throws Exception {
        InputStream inputStream = HtmlToImageUtl.class.getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("未找到 HTML 模板：" + filePath);
        }
        try (inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String resolveHtmlTemplatePath(String htmlName) {
        if (htmlName == null || htmlName.isBlank()) {
            throw new IllegalArgumentException("HTML 模板名不能为空");
        }
        String templatePath = htmlName.trim().replace("\\", "/");
        if (!templatePath.endsWith(HTML_SUFFIX)) {
            templatePath = templatePath + HTML_SUFFIX;
        }
        if (!templatePath.startsWith(TEMPLATE_DIR)) {
            templatePath = TEMPLATE_DIR + templatePath;
        }
        return templatePath;
    }

    private static Path buildDefaultOutputPath(String htmlName) throws Exception {
        Files.createDirectories(DEFAULT_OUTPUT_DIR);
        String fileName = sanitizeFileName(stripHtmlSuffix(htmlName))
                + "-"
                + OUTPUT_TIME_FORMATTER.format(LocalDateTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + ".png";
        return DEFAULT_OUTPUT_DIR.resolve(fileName);
    }

    private static String stripHtmlSuffix(String htmlName) {
        String fileName = htmlName.trim().replace("\\", "/");
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        if (fileName.endsWith(HTML_SUFFIX)) {
            return fileName.substring(0, fileName.length() - HTML_SUFFIX.length());
        }
        return fileName;
    }

    private static String sanitizeFileName(String fileName) {
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.isBlank()) {
            return "html-image";
        }
        return sanitized;
    }

    static String injectVueData(String htmlFilePath, String htmlContent, Object data) throws Exception {
        String encodedJson = Base64.getEncoder().encodeToString(OBJECT_MAPPER.writeValueAsBytes(data));
        String script = "<script>window.__BOT_DATA__ = JSON.parse(new TextDecoder('utf-8').decode("
                + "Uint8Array.from(atob('" + encodedJson + "'), function (c) { return c.charCodeAt(0); })));</script>";
        String base = buildBaseTag(htmlFilePath);
        String injection = base + CJK_FONT_FAMILY + script + buildVueScriptIfMissing(htmlContent);
        if (htmlContent.contains("<head>")) {
            htmlContent = htmlContent.replace("<head>", "<head>\n" + injection);
        } else if (htmlContent.contains("</head>")) {
            htmlContent = htmlContent.replace("</head>", injection + "\n</head>");
        } else {
            htmlContent = injection + htmlContent;
        }
        return injectVueAutoMount(htmlContent);
    }

    private static String injectVueAutoMount(String htmlContent) {
        StringBuilder script = new StringBuilder();
        if (htmlContent.contains("new Vue(") || htmlContent.contains("Vue.createApp(") || htmlContent.contains("createApp(")) {
            if (htmlContent.contains("</body>")) {
                return htmlContent.replace("</body>", script + "\n</body>");
            }
            return htmlContent + "\n" + script;
        }
        script.append("""
                <script>
                (function () {
                    if (!window.Vue || !document.getElementById('main')) {
                        window.__BOT_RENDERED__ = true;
                        return;
                    }
                    if (document.getElementById('main').__vue__) {
                        window.__BOT_RENDERED__ = true;
                        return;
                    }
                    var vm = new Vue({
                        el: '#main',
                        data: function () {
                            return window.__BOT_DATA__ || {};
                        }
                    });
                    if (window.Vue.nextTick) {
                        window.Vue.nextTick(function () {
                            window.__BOT_RENDERED__ = true;
                        });
                    } else {
                        setTimeout(function () {
                            window.__BOT_RENDERED__ = true;
                        }, 0);
                    }
                })();
                </script>
                """);
        if (htmlContent.contains("</body>")) {
            return htmlContent.replace("</body>", script + "\n</body>");
        }
        return htmlContent + "\n" + script;
    }

    private static String buildVueScriptIfMissing(String htmlContent) {
        if (htmlContent.contains("vue.js") || htmlContent.contains("Vue.js") || htmlContent.contains("unpkg.com/vue")) {
            return "";
        }
        return buildVueScriptTag();
    }

    private static void waitForRenderReady(Page page) {
        try {
            page.waitForFunction(
                    "window.__BOT_RENDERED__ === true",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(3000)
            );
        } catch (PlaywrightException e) {
            page.waitForTimeout(500);
        }
    }

    private static String buildVueScriptTag() {
        try {
            String vueScript = readHtmlFromResources("static/js/vue.js")
                    .replace("</script>", "<\\/script>");
            return "<script>\n" + vueScript + "\n</script>\n";
        } catch (Exception e) {
            return "<script src=\"js/vue.js\"></script>\n";
        }
    }

    private static String inlineKnownScripts(String htmlContent) {
        htmlContent = inlineScript(htmlContent, "js/echarts.min.js", "static/js/echarts.min.js");
        return htmlContent;
    }

    private static String inlineScript(String htmlContent, String scriptSrc, String resourcePath) {
        String scriptTag = "<script src=\"" + scriptSrc + "\"></script>";
        if (!htmlContent.contains(scriptTag)) {
            return htmlContent;
        }
        try {
            String scriptContent = readHtmlFromResources(resourcePath)
                    .replace("</script>", "<\\/script>");
            return htmlContent.replace(scriptTag, "<script>\n" + scriptContent + "\n</script>");
        } catch (Exception e) {
            return htmlContent;
        }
    }

    private static String buildBaseTag(String htmlFilePath) {
        String normalized = htmlFilePath.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        String directory = lastSlash < 0 ? "" : normalized.substring(0, lastSlash + 1);
        return "<base href=\"http://bot.local/" + directory + "\">\n";
    }

    private static void routePageResource(Route route) {
        try {
            URI uri = URI.create(route.request().url());
            if ("http".equalsIgnoreCase(uri.getScheme()) && "bot.local".equalsIgnoreCase(uri.getHost())) {
                fulfillClasspathResource(route);
                return;
            }
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                route.abort();
                return;
            }
            route.resume();
        } catch (Exception exception) {
            route.abort();
        }
    }

    private static void fulfillClasspathResource(Route route) {
        try {
            URI uri = URI.create(route.request().url());
            String resourcePath = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
            while (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            if (!resourcePath.startsWith("static/") || resourcePath.contains("..")) {
                route.fulfill(new Route.FulfillOptions().setStatus(404));
                return;
            }
            try (InputStream input = HtmlToImageUtl.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (input == null) {
                    route.fulfill(new Route.FulfillOptions().setStatus(404));
                    return;
                }
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType(contentType(resourcePath))
                        .setBodyBytes(input.readAllBytes()));
            }
        } catch (Exception e) {
            route.fulfill(new Route.FulfillOptions().setStatus(500));
        }
    }

    private static String contentType(String resourcePath) {
        String lower = resourcePath.toLowerCase();
        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".woff2")) {
            return "font/woff2";
        }
        return "application/octet-stream";
    }

    private static void assertPageResourcesLoaded(Page page) {
        List<?> brokenImages = (List<?>) page.evaluate("""
                Array.from(document.images)
                    .filter(function (image) { return !image.complete || image.naturalWidth === 0; })
                    .map(function (image) {
                        var value = image.currentSrc || image.getAttribute('src') || '(empty src)';
                        if (value.indexOf('data:') === 0) {
                            var comma = value.indexOf(',');
                            return (comma >= 0 ? value.substring(0, comma + 1) : 'data:') + '...';
                        }
                        try {
                            var url = new URL(value, document.baseURI);
                            return url.protocol + '//' + url.host + url.pathname;
                        } catch (ignored) {
                            return String(value).substring(0, 300);
                        }
                    })
                """);
        if (!brokenImages.isEmpty()) {
            throw new IllegalStateException("HTML 模板存在 " + brokenImages.size()
                    + " 个未加载图片资源，resources=>" + brokenImages);
        }
        List<?> unloadedStylesheets = (List<?>) page.evaluate("""
                Array.from(document.querySelectorAll('link[rel="stylesheet"]'))
                    .filter(function (link) { return !link.sheet; })
                    .map(function (link) {
                        var value = link.href || link.getAttribute('href') || '(empty href)';
                        try {
                            var url = new URL(value, document.baseURI);
                            return url.protocol + '//' + url.host + url.pathname;
                        } catch (ignored) {
                            return String(value).substring(0, 300);
                        }
                    })
                """);
        if (!unloadedStylesheets.isEmpty()) {
            throw new IllegalStateException("HTML 模板存在 " + unloadedStylesheets.size()
                    + " 个未加载样式表，resources=>" + unloadedStylesheets);
        }
    }
}
