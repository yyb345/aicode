package com.example.qa.controller;

import com.example.qa.tool.WeatherTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询控制器
 * 专门处理天气相关的API请求
 */
@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*")
public class WeatherController {

    @Autowired
    private WeatherTool weatherTool;

    /**
     * 获取当前天气
     */
    @GetMapping("/current")
    public Map<String, Object> getCurrentWeather(@RequestParam String city) {
        Map<String, Object> response = new HashMap<>();
        try {
            String weatherInfo = weatherTool.getCurrentWeather(city);
            response.put("weather", weatherInfo);
            response.put("city", city);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("error", "获取天气信息失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 获取天气预报
     */
    @GetMapping("/forecast")
    public Map<String, Object> getWeatherForecast(@RequestParam String city) {
        Map<String, Object> response = new HashMap<>();
        try {
            String forecastInfo = weatherTool.getWeatherForecast(city);
            response.put("forecast", forecastInfo);
            response.put("city", city);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("error", "获取天气预报失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 流式获取当前天气
     */
    @GetMapping(value = "/current/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getCurrentWeatherStream(@RequestParam String city) {
        try {
            String weatherInfo = weatherTool.getCurrentWeather(city);
            // 模拟流式输出
            return Flux.fromArray(weatherInfo.split(""))
                    .map(String::valueOf)
                    .map(content -> "data: " + content + "\n\n")
                    .delayElements(java.time.Duration.ofMillis(50));
        } catch (Exception e) {
            return Flux.just("data: 获取天气信息时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    /**
     * 流式获取天气预报
     */
    @GetMapping(value = "/forecast/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getWeatherForecastStream(@RequestParam String city) {
        try {
            String forecastInfo = weatherTool.getWeatherForecast(city);
            // 模拟流式输出
            return Flux.fromArray(forecastInfo.split(""))
                    .map(String::valueOf)
                    .map(content -> "data: " + content + "\n\n")
                    .delayElements(java.time.Duration.ofMillis(50));
        } catch (Exception e) {
            return Flux.just("data: 获取天气预报时出现错误：" + e.getMessage() + "\n\n");
        }
    }

    /**
     * 智能天气查询（根据问题自动判断查询类型）
     */
    @PostMapping("/query")
    public Map<String, Object> smartWeatherQuery(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        Map<String, Object> response = new HashMap<>();
        
        if (question == null || question.trim().isEmpty()) {
            response.put("error", "请输入有效的问题");
            response.put("status", "error");
            return response;
        }

        try {
            // 提取城市名称
            String city = extractCityFromQuestion(question);
            if (city == null) {
                response.put("error", "请指定要查询天气的城市名称");
                response.put("status", "error");
                return response;
            }

            // 判断是查询当前天气还是预报
            String result;
            String queryType;
            if (question.contains("预报") || question.contains("未来") || question.contains("明天") || question.contains("后天")) {
                result = weatherTool.getWeatherForecast(city);
                queryType = "forecast";
            } else {
                result = weatherTool.getCurrentWeather(city);
                queryType = "current";
            }

            response.put("result", result);
            response.put("city", city);
            response.put("queryType", queryType);
            response.put("status", "success");
            return response;

        } catch (Exception e) {
            response.put("error", "获取天气信息时出现错误：" + e.getMessage());
            response.put("status", "error");
            return response;
        }
    }

    /**
     * 流式智能天气查询
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> smartWeatherQueryStream(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        
        if (question == null || question.trim().isEmpty()) {
            return Flux.just("data: 请输入有效的问题\n\n");
        }

        try {
            // 提取城市名称
            String city = extractCityFromQuestion(question);
            if (city == null) {
                return Flux.just("data: 请指定要查询天气的城市名称\n\n");
            }

            // 判断是查询当前天气还是预报
            String result;
            if (question.contains("预报") || question.contains("未来") || question.contains("明天") || question.contains("后天")) {
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
     * 获取工具信息
     */
    @GetMapping("/tool/info")
    public Map<String, Object> getToolInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> weatherToolInfo = new HashMap<>();
            weatherToolInfo.put("description", weatherTool.getToolDescription());
            weatherToolInfo.put("functions", weatherTool.getSupportedFunctions());
            weatherToolInfo.put("available", weatherTool.isToolAvailable());
            
            response.put("weather_tool", weatherToolInfo);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("error", "获取工具信息失败：" + e.getMessage());
            response.put("status", "error");
        }
        return response;
    }

    /**
     * 检查工具状态
     */
    @GetMapping("/tool/status")
    public Map<String, Object> getToolStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isAvailable = weatherTool.isToolAvailable();
            response.put("available", isAvailable);
            response.put("status", isAvailable ? "success" : "error");
            response.put("message", isAvailable ? "天气工具正常" : "天气工具不可用");
        } catch (Exception e) {
            response.put("available", false);
            response.put("status", "error");
            response.put("message", "检查工具状态失败：" + e.getMessage());
        }
        return response;
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
