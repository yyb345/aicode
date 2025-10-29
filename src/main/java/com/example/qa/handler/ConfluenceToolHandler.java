package com.example.qa.handler;

import com.example.qa.tool.ConfluenceTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Confluence工具处理器
 * 处理与Confluence相关的查询请求
 */
@Component
public class ConfluenceToolHandler implements BusinessChainHandler {

    @Autowired
    private ConfluenceTool confluenceTool;

    @Override
    public boolean canHandle(String question) {
       return false;
    }

    @Override
    public Flux<String> handleStream(String question) {
        try {
            String result = getConfluenceResult(question);
            
            // 模拟流式输出
            return Flux.fromArray(result.split(""))
                    .map(String::valueOf)
                    .map(content -> content + "\n\n")
                    .delayElements(java.time.Duration.ofMillis(30));

        } catch (Exception e) {
            return Flux.just("data: 获取Confluence信息时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    @Override
    public String handleSync(String question) {
        try {
            return getConfluenceResult(question);
        } catch (Exception e) {
            return "获取Confluence信息时出现错误：" + e.getMessage();
        }
    }

    @Override
    public String getHandlerName() {
        return "ConfluenceToolHandler";
    }

    @Override
    public int getPriority() {
        return 10; // Confluence工具优先级较高
    }

    /**
     * 根据问题类型调用不同的Confluence方法
     */
    private String getConfluenceResult(String question) {
        // 根据问题类型调用不同的Confluence方法
        if (question.contains("最近") || question.contains("过去") || question.contains("更新")) {
            // 提取天数
            int days = extractDaysFromQuestion(question);
            if (days > 0) {
                return confluenceTool.getUpdatesForDays(days);
            } else {
                return confluenceTool.getRecentUpdates();
            }
        } else if (question.contains("页面") && question.contains("内容")) {
            // 提取页面ID
            String pageId = extractPageIdFromQuestion(question);
            if (pageId != null) {
                String result = confluenceTool.getPageContent(pageId);
                return result != null ? result : "无法获取页面内容，请检查页面ID是否正确";
            } else {
                return "请提供页面ID来获取页面内容";
            }
        } else {
            // 默认获取最近更新
            return confluenceTool.getRecentUpdates();
        }
    }

    /**
     * 从问题中提取天数
     */
    private int extractDaysFromQuestion(String question) {
        // 简单的天数提取逻辑
        if (question.contains("7天") || question.contains("一周")) {
            return 7;
        } else if (question.contains("3天")) {
            return 3;
        } else if (question.contains("1天") || question.contains("今天")) {
            return 1;
        } else if (question.contains("30天") || question.contains("一个月")) {
            return 30;
        } else if (question.contains("14天") || question.contains("两周")) {
            return 14;
        }
        
        // 尝试提取数字
        String[] words = question.split("\\s+");
        for (String word : words) {
            try {
                int days = Integer.parseInt(word);
                if (days > 0 && days <= 365) {
                    return days;
                }
            } catch (NumberFormatException e) {
                // 忽略非数字
            }
        }
        
        return 0; // 默认值
    }

    /**
     * 从问题中提取页面ID
     */
    private String extractPageIdFromQuestion(String question) {
        // 尝试提取引号中的内容
        if (question.contains("\"") || question.contains("'")) {
            String[] parts = question.split("[\"']");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        // 尝试提取数字ID
        String[] words = question.split("\\s+");
        for (String word : words) {
            if (word.matches("\\d+")) {
                return word;
            }
        }
        
        return null;
    }
}

