package com.example.qa.controller;

import com.example.qa.service.OpenAIService;
import com.example.qa.service.SpringAIService;
import com.example.qa.tool.WeatherTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

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
    private WeatherTool weatherTool;

    /**
     * 流式问答接口（支持工具调用）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAnswer(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("data: 请输入有效的问题\n\n");
        }

        try {
            // 检查是否需要调用工具
            if (shouldCallWeatherTool(question)) {
                return handleWeatherToolCall(question);
            }

            return openAIService.streamChat(question)
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
            // 检查是否需要调用工具
            if (shouldCallWeatherTool(question)) {
                String toolResult = handleWeatherToolCallSync(question);
                response.put("answer", toolResult);
                response.put("tool_used", "weather");
                return response;
            }

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
     * 检查是否需要调用天气工具
     */
    private boolean shouldCallWeatherTool(String question) {
        String lowerQuestion = question.toLowerCase();
        return lowerQuestion.contains("天气") || 
               lowerQuestion.contains("weather") ||
               lowerQuestion.contains("温度") ||
               lowerQuestion.contains("下雨") ||
               lowerQuestion.contains("晴天") ||
               lowerQuestion.contains("阴天") ||
               lowerQuestion.contains("预报");
    }

    /**
     * 处理天气工具调用（流式）
     */
    private Flux<String> handleWeatherToolCall(String question) {
        try {
            // 提取城市名称
            String city = extractCityFromQuestion(question);
            if (city == null) {
                return Flux.just("data: 请指定要查询天气的城市名称\n\n");
            }

            // 判断是查询当前天气还是预报
            String result;
            if (question.contains("预报") || question.contains("未来")) {
                result = weatherTool.getWeatherForecast(city);
            } else {
                result = weatherTool.getCurrentWeather(city);
            }

            // 模拟流式输出
            return Flux.fromArray(result.split(""))
                    .map(String::valueOf)
                    .map(content -> "data: " + content + "\n\n")
                    .delayElements(java.time.Duration.ofMillis(50));

        } catch (Exception e) {
            return Flux.just("data: 获取天气信息时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    /**
     * 处理天气工具调用（同步）
     */
    private String handleWeatherToolCallSync(String question) {
        try {
            // 提取城市名称
            String city = extractCityFromQuestion(question);
            if (city == null) {
                return "请指定要查询天气的城市名称";
            }

            // 判断是查询当前天气还是预报
            if (question.contains("预报") || question.contains("未来")) {
                return weatherTool.getWeatherForecast(city);
            } else {
                return weatherTool.getCurrentWeather(city);
            }

        } catch (Exception e) {
            return "获取天气信息时出现错误：" + e.getMessage();
        }
    }

    /**
     * 从问题中提取城市名称
     */
    private String extractCityFromQuestion(String question) {
        // 简单的城市名称提取逻辑
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "武汉", "成都", "西安", "重庆",
                           "beijing", "shanghai", "guangzhou", "shenzhen", "hangzhou", "nanjing", 
                           "wuhan", "chengdu", "xian", "chongqing"};
        
        String lowerQuestion = question.toLowerCase();
        for (String city : cities) {
            if (lowerQuestion.contains(city.toLowerCase())) {
                return city;
            }
        }
        
        // 如果没有找到预定义的城市，尝试提取引号中的内容
        if (question.contains("\"") || question.contains("'")) {
            String[] parts = question.split("[\"']");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        return null;
    }
}