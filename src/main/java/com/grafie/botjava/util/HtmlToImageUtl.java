package com.grafie.botjava.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 转换html生成图片
 *
 * @author grafie.chen
 * @since 2025/1/24  17:10
 */
public class HtmlToImageUtl {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.setViewportSize(1280, 720);
            page.setContent(htmlContent);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(500);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(outputPath))
                    .setFullPage(true));

            return outputPath;
        }
    }

    /**
     * 读取资源目录中的 HTML 文件内容
     *
     * @param filePath 资源目录下的文件路径
     * @return 文件内容
     * @throws Exception 如果文件读取失败
     */
    public static String readHtmlFromResources(String filePath) throws Exception {
        try (InputStream inputStream = HtmlToImageUtl.class.getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String injectVueData(String htmlFilePath, String htmlContent, Object data) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(data)
                .replace("</script>", "<\\/script>");
        String script = "<script>window.__BOT_DATA__ = " + json + ";</script>";
        String base = buildBaseTag(htmlFilePath);
        String injection = base + script;
        if (htmlContent.contains("</head>")) {
            return htmlContent.replace("</head>", injection + "\n</head>");
        }
        return injection + htmlContent;
    }

    private static String buildBaseTag(String htmlFilePath) {
        try {
            String parent = "";
            int lastSlash = htmlFilePath.lastIndexOf('/');
            if (lastSlash >= 0) {
                parent = htmlFilePath.substring(0, lastSlash + 1);
            }
            java.net.URL resourceUrl = HtmlToImageUtl.class.getClassLoader().getResource(parent);
            if (resourceUrl == null) {
                return "";
            }
            return "<base href=\"" + resourceUrl + "\">\n";
        } catch (Exception e) {
            return "";
        }
    }
}
