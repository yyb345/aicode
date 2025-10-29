package com.example.qa.tool;

import com.example.qa.service.OpenAIService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.example.qa.service.SpringAIService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Confluence文档爬取工具类
 * 用于爬取Confluence文档内容，获取过去7天的更新并进行总结
 */
@Component
public class ConfluenceTool {

    private static final Logger logger = LoggerFactory.getLogger(ConfluenceTool.class);

    @Value("${confluence.base-url}")
    private String confluenceBaseUrl;

    @Value("${confluence.session-cookie}")
    private String confluenceSessionCookie;

    @Value("${confluence.space-key}")
    private String confluenceSpaceKey;

    @Autowired
    private OpenAIService openAIService;

    private final WebClient webClient;

    public ConfluenceTool() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 获取过去7天的Confluence文档更新
     * @return 更新内容总结
     */
    public String getRecentUpdates() {
        try {
            logger.info("开始获取Confluence过去7天的更新内容");

            // 计算7天前的时间戳
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // 获取空间中的页面列表
            List<Map<String, Object>> pages = getSpacePages();
            if (pages.isEmpty()) {
                return "未找到任何页面内容";
            }

            // 筛选过去7天更新的页面
            List<Map<String, Object>> recentPages = filterRecentPages(pages, sevenDaysAgo);
            if (recentPages.isEmpty()) {
                return "过去7天内没有页面更新";
            }

            // 获取每个页面的详细内容
            List<String> pageContents = new ArrayList<>();
            for (Map<String, Object> page : recentPages) {
                String pageId = page.get("id").toString();
                String pageTitle = page.get("title").toString();
                String content = getPageContent(pageId);
                
                if (content != null && !content.trim().isEmpty()) {
                    pageContents.add("标题: " + pageTitle + "\n内容: " + content + "\n");
                }
            }

            if (pageContents.isEmpty()) {
                return "过去7天更新的页面内容为空";
            }

            // 使用AI服务进行总结
            String allContent = String.join("\n---\n", pageContents);
            String summary = generateSummary(allContent);

            return formatSummaryResponse(recentPages.size(), summary);

        } catch (Exception e) {
            logger.error("获取Confluence更新失败: {}", e.getMessage(), e);
            return "抱歉，获取Confluence更新时出现错误：" + e.getMessage();
        }
    }

    /**
     * 获取指定页面的内容
     * @param pageId 页面ID
     * @return 页面内容
     */
    public String getPageContent(String pageId) {
        try {
            logger.info("获取页面内容，页面ID: {}", pageId);

            String url = confluenceBaseUrl + "/rest/api/content/" + pageId + "?expand=body.storage,version";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, getCookieHeader())
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("body")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.get("body");
                @SuppressWarnings("unchecked")
                Map<String, Object> storage = (Map<String, Object>) body.get("storage");
                
                if (storage != null && storage.containsKey("value")) {
                    return cleanHtmlContent(storage.get("value").toString());
                }
            }

            return null;

        } catch (Exception e) {
            logger.error("获取页面内容失败，页面ID: {}, 错误: {}", pageId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取空间中的所有页面
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSpacePages() {
        try {
            // 先尝试获取所有内容，不限制空间

            String url = confluenceBaseUrl + "/rest/api/content?spaceKey="+confluenceSpaceKey+"&type=page&limit=100&expand=version";
            
            logger.info("尝试访问URL: {}", url);

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, getCookieHeader())
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            logger.info("API响应: {}", response);

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> allPages = (List<Map<String, Object>>) response.get("results");
                return allPages;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            logger.error("获取空间页面失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 筛选过去指定时间更新的页面
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterRecentPages(List<Map<String, Object>> pages, LocalDateTime since) {
        return pages.stream()
                .filter(page -> {
                    try {
                        Map<String, Object> version = (Map<String, Object>) page.get("version");
                        if (version != null && version.containsKey("when")) {
                            String whenStr = version.get("when").toString();
                            LocalDateTime pageUpdateTime = LocalDateTime.parse(whenStr.substring(0, 19));
                            return pageUpdateTime.isAfter(since);
                        }
                        return false;
                    } catch (Exception e) {
                        logger.warn("解析页面更新时间失败: {}", e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 使用AI服务生成内容总结
     */
    private String generateSummary(String content) {
        try {
            String prompt = "请对以下Confluence文档内容进行总结，提取关键信息和要点：\n\n" + content;
            return openAIService.chat(prompt);
        } catch (Exception e) {
            logger.error("AI总结生成失败: {}", e.getMessage());
            return "AI总结生成失败，原始内容长度: " + content.length() + " 字符";
        }
    }

    /**
     * 清理HTML内容，提取纯文本
     */
    private String cleanHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            return "";
        }
        
        // 简单的HTML标签清理
        return htmlContent
                .replaceAll("<[^>]+>", " ") // 移除HTML标签
                .replaceAll("&nbsp;", " ") // 替换HTML实体
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ") // 合并多个空格
                .trim();
    }


    /**
     * 生成Cookie认证头
     */
    private String getCookieHeader() {
        if (confluenceSessionCookie != null && !confluenceSessionCookie.isEmpty()) {
            logger.debug("使用Session Cookie认证");
            return confluenceSessionCookie;
        }
        return "";
    }

    /**
     * 格式化总结响应
     */
    private String formatSummaryResponse(int pageCount, String summary) {
        StringBuilder result = new StringBuilder();
        result.append("Confluence文档更新总结\n");
        result.append("时间范围：过去7天\n");
        result.append("更新页面数：").append(pageCount).append(" 个\n");
        result.append("内容总结：\n\n");
        result.append(summary);
        return result.toString();
    }

    /**
     * 获取工具描述信息
     */
    public String getToolDescription() {
        return "Confluence文档爬取工具 - 可以爬取Confluence文档内容，获取过去7天的更新并进行智能总结";
    }

    /**
     * 获取支持的功能列表
     */
    public List<String> getSupportedFunctions() {
        List<String> functions = new ArrayList<>();
        functions.add("getRecentUpdates() - 获取过去7天的更新内容并总结");
        functions.add("getPageContent(pageId) - 获取指定页面的内容");
        return functions;
    }

    /**
     * 检查工具是否可用
     */
    public boolean isToolAvailable() {
        try {
            // 尝试获取空间页面列表来验证连接
            List<Map<String, Object>> pages = getSpacePages();
            return !pages.isEmpty();
        } catch (Exception e) {
            logger.warn("Confluence工具可用性检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定时间范围内的更新
     * @param days 天数
     * @return 更新内容总结
     */
    public String getUpdatesForDays(int days) {
        try {
            logger.info("开始获取Confluence过去{}天的更新内容", days);

            LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
            List<Map<String, Object>> pages = getSpacePages();
            
            if (pages.isEmpty()) {
                return "未找到任何页面内容";
            }

            List<Map<String, Object>> recentPages = filterRecentPages(pages, sinceDate);
            if (recentPages.isEmpty()) {
                return "过去" + days + "天内没有页面更新";
            }

            List<String> pageContents = new ArrayList<>();
            for (Map<String, Object> page : recentPages) {
                String pageId = page.get("id").toString();
                String pageTitle = page.get("title").toString();
                String content = getPageContent(pageId);
                
                if (content != null && !content.trim().isEmpty()) {
                    pageContents.add("标题: " + pageTitle + "\n内容: " + content + "\n");
                }
            }

            if (pageContents.isEmpty()) {
                return "过去" + days + "天更新的页面内容为空";
            }

            String allContent = String.join("\n---\n", pageContents);
            String summary = generateSummary(allContent);

            return formatCustomSummaryResponse(days, recentPages.size(), summary);

        } catch (Exception e) {
            logger.error("获取Confluence更新失败: {}", e.getMessage(), e);
            return "抱歉，获取Confluence更新时出现错误：" + e.getMessage();
        }
    }

    /**
     * 格式化自定义时间范围的总结响应
     */
    private String formatCustomSummaryResponse(int days, int pageCount, String summary) {
        StringBuilder result = new StringBuilder();
        result.append("Confluence文档更新总结\n");
        result.append("时间范围：过去").append(days).append("天\n");
        result.append("更新页面数：").append(pageCount).append(" 个\n");
        result.append("内容总结：\n\n");
        result.append(summary);
        return result.toString();
    }
    
    /**
     * 获取页面基本信息
     * @param pageId 页面ID
     * @return 页面基本信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPageInfo(String pageId) {
        try {
            logger.info("获取页面信息，页面ID: {}", pageId);

            String url = confluenceBaseUrl + "/rest/api/content/" + pageId + "?expand=version,space";

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, getCookieHeader())
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return response;
            }

            return null;

        } catch (Exception e) {
            logger.error("获取页面信息失败，页面ID: {}, 错误: {}", pageId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取所有页面（支持分页）
     * @param limit 每页数量限制
     * @param start 起始位置
     * @return 页面列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllPages(int limit, int start) {
        try {
            String url = confluenceBaseUrl + "/rest/api/content?spaceKey=" + confluenceSpaceKey + 
                        "&type=page&limit=" + limit + "&start=" + start + "&expand=version,space";
            
            logger.info("获取所有页面，URL: {}", url);

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, getCookieHeader())
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("results")) {
                return (List<Map<String, Object>>) response.get("results");
            }

            return new ArrayList<>();

        } catch (Exception e) {
            logger.error("获取所有页面失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取所有页面（默认参数）
     */
    public List<Map<String, Object>> getAllPages() {
        return getAllPages(100, 0);
    }
    
    /**
     * 检查Confluence连接是否可用
     */
    public boolean isConnectionAvailable() {
        try {
            List<Map<String, Object>> pages = getSpacePages();
            return !pages.isEmpty();
        } catch (Exception e) {
            logger.warn("Confluence连接检查失败: {}", e.getMessage());
            return false;
        }
    }
}
