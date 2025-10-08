package com.example.qa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * OpenAI服务类
 */
@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private final WebClient webClient;

    public OpenAIService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 流式调用OpenAI API
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

        logger.info("发送流式请求到: {}", baseUrl + "/chat/completions");
        logger.info("请求体: {}", requestBody);
        logger.info("API Key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> logger.info("收到流式数据块: {}", chunk))
                .map(this::processSSEResponse)
                .filter(response -> response != null && !response.trim().isEmpty());
    }

    /**
     * 普通调用OpenAI API
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

        logger.info("发送请求到: {}", baseUrl + "/chat/completions");
        logger.info("请求体: {}", requestBody);
        logger.info("API Key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            logger.info("收到响应: {}", response);

            if (response != null && response.containsKey("choices")) {
                Object choicesObj = response.get("choices");
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
            return "抱歉，无法获取回答";
        } catch (Exception e) {
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
            int choicesStart = jsonData.indexOf("\"choices\":[");
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
        } catch (Exception e) {
            logger.warn("解析JSON数据时出错: {}", e.getMessage());
        }
        return "";
    }
}
