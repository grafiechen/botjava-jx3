package com.grafie.botjava.util;

import com.microsoft.playwright.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.stream.Collectors;
/**
 * 转换html生成图片
 *
 * @author grafie.chen
 * @since 2025/1/24  17:10
 */
public class HtmlToImageUtl {

    /**
     * 渲染 HTML 文件为图片
     *
     * @param htmlFilePath 资源目录下 HTML 文件路径
     * @param outputPath   输出图片路径
     * @return 输出图片路径
     * @throws Exception 如果操作失败
     */
    public static String renderHtmlToImage(String htmlFilePath, String outputPath) throws Exception {
        // 1. 读取 HTML 文件内容
        String htmlContent = readHtmlFromResources(htmlFilePath);

        // 2. 初始化 Playwright
        try (Playwright playwright = Playwright.create()) {
            // 启动浏览器
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // 3. 设置 HTML 内容
            page.setContent(htmlContent);

            // 4. 设置页面视窗大小（可根据需求调整）
            page.setViewportSize(1280, 720);

            // 5. 截图并保存图片
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(outputPath))
                    .setFullPage(true));

            // 6. 返回图片路径
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
}
