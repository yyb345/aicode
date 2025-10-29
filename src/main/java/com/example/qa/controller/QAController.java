package com.example.qa.controller;

import com.example.qa.handler.BusinessChainHandler;
import com.example.qa.service.MaterialsContextService;
import com.example.qa.service.OpenAIService;
import com.example.qa.service.SpringAIService;
import com.example.qa.tool.ConfluenceTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 问答控制器
 * 处理智能问答请求，支持工具调用
 */
@RestController
@RequestMapping("/api/qa")
@CrossOrigin(origins = "*")
public class QAController {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private SpringAIService springAIService;

    @Autowired
    private ConfluenceTool confluenceTool;

    @Autowired
    private List<BusinessChainHandler> handlers;

    @Autowired
    private MaterialsContextService materialsContextService;

    /**
     * 获取按优先级排序的处理器列表
     */
    private List<BusinessChainHandler> getSortedHandlers() {
        return handlers.stream()
                .sorted(Comparator.comparingInt(BusinessChainHandler::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 查找可以处理问题的处理器
     */
    private BusinessChainHandler findHandler(String question) {
        return getSortedHandlers().stream()
                .filter(handler -> handler.canHandle(question))
                .findFirst()
                .orElse(null);
    }

    /**
     * 跳过标识常量
     */
    private static final String SKIP_MARKER = "__SKIP__";

    /**
     * 流式问答接口（支持工具调用，支持跳过逻辑）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAnswer(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("data: 请输入有效的问题\n\n");
        }

        try {
            // 使用责任链模式查找合适的处理器，支持跳过逻辑
            List<BusinessChainHandler> sortedHandlers = getSortedHandlers();
            return processHandlersWithSkip(question, sortedHandlers, 0);
        } catch (Exception e) {
            String errorMsg = "抱歉，处理您的问题时出现了错误：" + e.getMessage();
            return Flux.just("data: " + errorMsg + "\n\n");
        }
    }

    /**
     * 处理handler链，支持跳过逻辑
     */
    private Flux<String> processHandlersWithSkip(String question, 
                                                  List<BusinessChainHandler> handlers, 
                                                  int startIndex) {
        // 如果已经遍历完所有handler，使用默认问答
        if (startIndex >= handlers.size()) {
            return getDefaultAnswerStream(question);
        }

        // 查找当前及后续可以处理的handler
        for (int i = startIndex; i < handlers.size(); i++) {
            BusinessChainHandler handler = handlers.get(i);
            if (handler.canHandle(question)) {
                final int currentIndex = i; // 保存为final变量供lambda使用
                Flux<String> handlerResult = handler.handleStream(question);
                
                // 使用cache来缓存结果，这样我们可以多次订阅
                Flux<String> cachedResult = handlerResult.cache();
                
                // 检查第一个chunk是否包含跳过标识
                return cachedResult
                        .take(1)
                        .collectList()
                        .flatMapMany(chunks -> {
                            boolean isSkipped = false;
                            if (chunks.isEmpty()) {
                                isSkipped = true; // 空结果视为跳过
                            } else {
                                // 合并所有chunk并检查
                                String combined = String.join("", chunks);
                                String clean = combined.replace("\n\n", "").replace("\n", "").trim();
                                isSkipped = clean.contains(SKIP_MARKER);
                            }
                            
                            if (isSkipped) {
                                // 跳过，尝试下一个handler
                                return processHandlersWithSkip(question, handlers, currentIndex + 1);
                            } else {
                                // 不跳过，先添加handler提示信息，然后返回完整的handler结果（过滤掉SKIP标记）
                                String handlerName = getFriendlyHandlerName(handler.getHandlerName());
                                String handlerInfo = "💡 我使用了 " + handlerName + " 来回答您的问题：\n\n";
                                return Flux.just(handlerInfo)
                                        .concatWith(cachedResult
                                                .filter(chunk -> !chunk.trim().equals(SKIP_MARKER) && !chunk.contains(SKIP_MARKER)));
                            }
                        })
                        .defaultIfEmpty("")
                        .switchIfEmpty(processHandlersWithSkip(question, handlers, currentIndex + 1));
            }
        }

        // 没有找到可以处理的handler，使用默认问答
        return getDefaultAnswerStream(question);
    }

    /**
     * 获取友好的handler名称（去掉Handler后缀，格式化）
     */
    private String getFriendlyHandlerName(String handlerName) {
        if (handlerName == null) {
            return "未知处理器";
        }
        // 去掉Handler后缀
        String friendly = handlerName.replaceAll("Handler$", "");
        // 将驼峰命名转换为更友好的格式
        friendly = friendly.replaceAll("([a-z])([A-Z])", "$1 $2");
        return friendly;
    }

    /**
     * 获取默认问答流
     */
    private Flux<String> getDefaultAnswerStream(String question) {
        return openAIService.streamChat(question)
                .map(content -> {
                    if (content == null || content.trim().isEmpty()) {
                        return "";
                    }
                    return content + "\n\n";
                })
                .filter(content -> !content.isEmpty())
                .onErrorResume(throwable -> {
                    String errorMsg = "抱歉，处理您的问题时出现了错误：" + throwable.getMessage();
                    return Flux.just("data: " + errorMsg + "\n\n");
                });
    }

    /**
     * 普通问答接口（非流式，支持工具调用）
     */
    @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        Map<String, String> response = new HashMap<>();
        
        if (question == null || question.trim().isEmpty()) {
            response.put("answer", "请输入有效的问题");
            return response;
        }

        try {
            // 使用责任链模式查找合适的处理器
            BusinessChainHandler handler = findHandler(question);
            if (handler != null) {
                String result = handler.handleSync(question);
                response.put("answer", result);
                response.put("tool_used", handler.getHandlerName().toLowerCase().replace("handler", ""));
                return response;
            }

            // 如果没有找到合适的处理器，使用默认的OpenAI服务
            String answer = openAIService.chat(question);
            response.put("answer", answer);
            return response;
        } catch (Exception e) {
            response.put("answer", "抱歉，处理您的问题时出现了错误：" + e.getMessage());
            return response;
        }
    }

    /**
     * Spring AI Alibaba流式问答接口
     */
    @PostMapping(value = "/spring-ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> springAIStreamAnswer(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("data: 请输入有效的问题\n\n");
        }

        try {
            return springAIService.streamChat(question)
                    .map(content -> {
                        // 如果内容为空，返回空字符串（会被过滤掉）
                        if (content == null || content.trim().isEmpty()) {
                            return "";
                        }
                        // 返回正确的SSE格式
                        return content + "\n\n";
                    })
                    .filter(content -> !content.isEmpty()) // 过滤空内容
                    .onErrorResume(throwable -> {
                        String errorMsg = "抱歉，处理您的问题时出现了错误：" + throwable.getMessage();
                        return Flux.just("data: " + errorMsg + "\n\n");
                    });
        } catch (Exception e) {
            String errorMsg = "抱歉，处理您的问题时出现了错误：" + e.getMessage();
            return Flux.just("data: " + errorMsg + "\n\n");
        }
    }

    /**
     * Spring AI Alibaba普通问答接口（非流式）
     */
    @PostMapping("/spring-ai/ask")
    public Map<String, String> springAIAsk(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        Map<String, String> response = new HashMap<>();
        
        if (question == null || question.trim().isEmpty()) {
            response.put("answer", "请输入有效的问题");
            return response;
        }

        try {
            String answer = springAIService.chat(question);
            response.put("answer", answer);
            return response;
        } catch (Exception e) {
            response.put("answer", "抱歉，处理您的问题时出现了错误：" + e.getMessage());
            return response;
        }
    }

    /**
     * 获取Spring AI Alibaba支持的模型列表
     */
    @GetMapping("/spring-ai/models")
    public Map<String, Object> getSpringAIModels() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("models", springAIService.getSupportedModels());
            response.put("status", "success");
        } catch (Exception e) {
            response.put("error", "获取模型列表失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 检查Spring AI Alibaba服务状态
     */
    @GetMapping("/spring-ai/status")
    public Map<String, Object> getSpringAIStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isAvailable = springAIService.isServiceAvailable();
            response.put("available", isAvailable);
            response.put("status", isAvailable ? "success" : "error");
            response.put("message", isAvailable ? "服务正常" : "服务不可用");
        } catch (Exception e) {
            response.put("available", false);
            response.put("status", "error");
            response.put("message", "检查服务状态失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 工具调用相关方法
     */

    /**
     * Confluence工具调用相关方法
     */

    /**
     * 检查是否需要调用Confluence工具
     */
    private boolean shouldCallConfluenceTool(String question) {
        String lowerQuestion = question.toLowerCase();
        return lowerQuestion.contains("confluence") || 
               lowerQuestion.contains("文档") ||
               lowerQuestion.contains("更新") ||
               lowerQuestion.contains("总结") ||
               lowerQuestion.contains("最近") ||
               lowerQuestion.contains("过去") ||
               lowerQuestion.contains("页面") ||
               lowerQuestion.contains("内容") ||
               lowerQuestion.contains("知识库") ||
               lowerQuestion.contains("wiki");
    }

    /**
     * 处理Confluence工具调用（流式）
     */
    private Flux<String> handleConfluenceToolCall(String question) {
        try {
            String result;
            
            // 根据问题类型调用不同的Confluence方法
            if (question.contains("最近") || question.contains("过去") || question.contains("更新")) {
                // 提取天数
                int days = extractDaysFromQuestion(question);
                if (days > 0) {
                    result = confluenceTool.getUpdatesForDays(days);
                } else {
                    result = confluenceTool.getRecentUpdates();
                }
            } else if (question.contains("页面") && question.contains("内容")) {
                // 提取页面ID
                String pageId = extractPageIdFromQuestion(question);
                if (pageId != null) {
                    result = confluenceTool.getPageContent(pageId);
                    if (result == null) {
                        result = "无法获取页面内容，请检查页面ID是否正确";
                    }
                } else {
                    result = "请提供页面ID来获取页面内容";
                }
            } else {
                // 默认获取最近更新
                result = confluenceTool.getRecentUpdates();
            }

            // 模拟流式输出
            return Flux.fromArray(result.split(""))
                    .map(String::valueOf)
                    .map(content ->  content + "\n\n")
                    .delayElements(java.time.Duration.ofMillis(30));

        } catch (Exception e) {
            return Flux.just("data: 获取Confluence信息时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    /**
     * 处理Confluence工具调用（同步）
     */
    private String handleConfluenceToolCallSync(String question) {
        try {
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

        } catch (Exception e) {
            return "获取Confluence信息时出现错误：" + e.getMessage();
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

    /**
     * 获取Confluence工具状态
     */
    @GetMapping("/confluence/status")
    public Map<String, Object> getConfluenceStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isAvailable = confluenceTool.isToolAvailable();
            response.put("available", isAvailable);
            response.put("status", isAvailable ? "success" : "error");
            response.put("message", isAvailable ? "Confluence工具正常" : "Confluence工具不可用");
            response.put("description", confluenceTool.getToolDescription());
            response.put("functions", confluenceTool.getSupportedFunctions());
        } catch (Exception e) {
            response.put("available", false);
            response.put("status", "error");
            response.put("message", "检查Confluence工具状态失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 获取Confluence最近更新
     */
    @GetMapping("/confluence/recent")
    public Map<String, String> getConfluenceRecentUpdates() {
        Map<String, String> response = new HashMap<>();
        try {
            String result = confluenceTool.getRecentUpdates();
            response.put("result", result);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("result", "获取Confluence最近更新失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 获取指定天数的Confluence更新
     */
    @GetMapping("/confluence/updates/{days}")
    public Map<String, String> getConfluenceUpdates(@PathVariable int days) {
        Map<String, String> response = new HashMap<>();
        try {
            if (days <= 0 || days > 365) {
                response.put("result", "天数必须在1-365之间");
                response.put("status", "error");
                return response;
            }
            
            String result = confluenceTool.getUpdatesForDays(days);
            response.put("result", result);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("result", "获取Confluence更新失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 获取指定页面的内容
     */
    @GetMapping("/confluence/page/{pageId}")
    public Map<String, String> getConfluencePageContent(@PathVariable String pageId) {
        Map<String, String> response = new HashMap<>();
        try {
            String result = confluenceTool.getPageContent(pageId);
            if (result != null) {
                response.put("result", result);
                response.put("status", "success");
            } else {
                response.put("result", "无法获取页面内容，请检查页面ID是否正确");
                response.put("status", "error");
            }
        } catch (Exception e) {
            response.put("result", "获取页面内容失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 材料文档相关API接口
     */

    /**
     * 获取材料文档统计信息
     */
    @GetMapping("/materials/statistics")
    public Map<String, String> getMaterialsStatistics() {
        Map<String, String> response = new HashMap<>();
        try {
            String stats = materialsContextService.getMaterialsStatistics();
            response.put("statistics", stats);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("statistics", "获取材料文档统计信息失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 搜索相关材料文档
     */
    @GetMapping("/materials/search")
    public Map<String, String> searchMaterials(@RequestParam String keyword) {
        Map<String, String> response = new HashMap<>();
        try {
            String context = materialsContextService.getRelevantDocumentsAsContext(keyword);
            response.put("result", context);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("result", "搜索材料文档失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 根据分类获取材料文档
     */
    @GetMapping("/materials/category/{category}")
    public Map<String, String> getMaterialsByCategory(@PathVariable String category) {
        Map<String, String> response = new HashMap<>();
        try {
            String context = materialsContextService.getDocumentsByCategoryAsContext(category);
            response.put("result", context);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("result", "获取分类材料文档失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }
}