package com.example.qa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

/**
 * Spring AI Alibaba服务类
 * 使用原生HTTP客户端调用DashScope API实现与OpenAIService相同的功能
 */
@Service
public class SpringAIService {

    private static final Logger logger = LoggerFactory.getLogger(SpringAIService.class);

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    @Value("${spring.ai.alibaba.endpoint}")
    private String endpoint;

    @Value("${spring.ai.alibaba.model:qwen-turbo}")
    private String model;

    private final WebClient webClient;

    public SpringAIService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 流式调用DashScope API
     */
    public Flux<String> streamChat(String question) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", question);
        requestBody.put("messages", Arrays.asList(message));
        
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        logger.info("使用Spring AI Alibaba发送流式请求到: {}", endpoint + "/services/aigc/text-generation/generation");
        logger.info("请求体: {}", requestBody);
        logger.info("API Key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        return webClient.post()
                .uri(endpoint + "/services/aigc/text-generation/generation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> logger.info("收到流式数据块: {}", chunk))
                .map(this::processSSEResponse)
                .filter(response -> response != null && !response.trim().isEmpty());
    }

    /**
     * 普通调用DashScope API
     */
    public String chat(String question) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", question);
        requestBody.put("messages", Arrays.asList(message));
        
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        logger.info("使用Spring AI Alibaba发送请求到: {}", endpoint + "/services/aigc/text-generation/generation");
        logger.info("请求体: {}", requestBody);
        logger.info("API Key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(endpoint + "/services/aigc/text-generation/generation")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            logger.info("收到响应: {}", response);

            if (response != null && response.containsKey("output")) {
                Object outputObj = response.get("output");
                if (outputObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> output = (Map<String, Object>) outputObj;
                    Object choicesObj = output.get("choices");
                    if (choicesObj instanceof java.util.List) {
                        java.util.List<?> choices = (java.util.List<?>) choicesObj;
                        if (!choices.isEmpty()) {
                            Object choiceObj = choices.get(0);
                            if (choiceObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> choice = (Map<String, Object>) choiceObj;
                                Object messageObj = choice.get("message");
                                if (messageObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                                    Object content = messageMap.get("content");
                                    return content != null ? content.toString() : "无法获取回答内容";
                                }
                            }
                        }
                    }
                }
            }
            return "抱歉，无法获取回答";
        } catch (Exception e) {
            logger.error("普通调用异常: {}", e.getMessage(), e);
            return "抱歉，处理您的问题时出现了错误：" + e.getMessage();
        }
    }

    /**
     * 处理SSE响应，提取内容并重新格式化为正确的SSE格式
     */
    private String processSSEResponse(String response) {
        try {
            // 检查是否是结束标记
            if (response.equals("[DONE]")) {
                return "";
            }
            
            // 处理可能的多行响应（SSE格式）
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("data: ")) {
                    String data = line.substring(6); // 跳过 "data: "
                    if (data.equals("[DONE]")) {
                        return "";
                    }
                    // 尝试解析JSON数据并提取内容
                    String content = extractContentFromJson(data);
                    if (!content.isEmpty()) {
                        return content;
                    }
                } else if (line.startsWith("data:")) {
                    String data = line.substring(5); // 跳过 "data:"
                    if (data.trim().equals("[DONE]")) {
                        return "";
                    }
                    // 尝试解析JSON数据并提取内容
                    String content = extractContentFromJson(data);
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
            }
            
            // 如果没有找到data:前缀，直接尝试解析JSON
            String content = extractContentFromJson(response);
            if (!content.isEmpty()) {
                return content;
            }
            
        } catch (Exception e) {
            logger.warn("处理SSE响应时出错: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 从JSON字符串中提取内容
     */
    private String extractContentFromJson(String jsonData) {
        try {
            // 直接解析JSON响应
            int outputStart = jsonData.indexOf("\"output\":{");
            if (outputStart != -1) {
                int choicesStart = jsonData.indexOf("\"choices\":[", outputStart);
                if (choicesStart != -1) {
                    int deltaStart = jsonData.indexOf("\"delta\":{", choicesStart);
                    if (deltaStart != -1) {
                        int contentStart = jsonData.indexOf("\"content\":\"", deltaStart);
                        if (contentStart != -1) {
                            contentStart += 11; // 跳过 "content":"
                            int contentEnd = jsonData.indexOf("\"", contentStart);
                            if (contentEnd != -1) {
                                String content = jsonData.substring(contentStart, contentEnd);
                                // 处理转义字符
                                content = content.replace("\\n", "\n")
                                               .replace("\\t", "\t")
                                               .replace("\\\"", "\"")
                                               .replace("\\\\", "\\");
                                logger.info("提取到内容: '{}'", content);
                                return content;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("解析JSON数据时出错: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 获取支持的模型列表
     */
    public List<String> getSupportedModels() {
        return Arrays.asList(
                "qwen-turbo",
                "qwen-plus", 
                "qwen-max",
                "qwen-max-longcontext"
        );
    }

    /**
     * 检查服务状态
     */
    public boolean isServiceAvailable() {
        try {
            // 发送一个简单的测试请求
            String testResponse = chat("你好");
            return testResponse != null && !testResponse.contains("错误");
        } catch (Exception e) {
            logger.warn("服务状态检查失败: {}", e.getMessage());
            return false;
        }
    }
}