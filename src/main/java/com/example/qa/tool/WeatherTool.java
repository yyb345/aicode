package com.example.qa.tool;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 天气查询工具类
 * 使用MCP (Model Context Protocol) 实现天气查询功能
 */
@Component
public class WeatherTool {

    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);

    // 使用免费的天气API服务
    private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_API_URL = "https://api.openweathermap.org/data/2.5/forecast";
    
    @Value("${weather.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public WeatherTool() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 获取当前天气信息
     * @param city 城市名称
     * @return 天气信息字符串
     */
    public String getCurrentWeather(String city) {
        try {
            logger.info("查询城市 {} 的当前天气", city);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(WEATHER_API_URL)
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "zh_cn")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return formatCurrentWeatherResponse(response);
            } else {
                return "抱歉，无法获取 " + city + " 的天气信息";
            }

        } catch (Exception e) {
            logger.error("获取天气信息失败: {}", e.getMessage(), e);
            return "抱歉，获取天气信息时出现错误：" + e.getMessage();
        }
    }

    /**
     * 获取天气预报信息
     * @param city 城市名称
     * @return 天气预报信息字符串
     */
    public String getWeatherForecast(String city) {
        try {
            logger.info("查询城市 {} 的天气预报", city);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(FORECAST_API_URL)
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "zh_cn")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return formatForecastResponse(response);
            } else {
                return "抱歉，无法获取 " + city + " 的天气预报信息";
            }

        } catch (Exception e) {
            logger.error("获取天气预报失败: {}", e.getMessage(), e);
            return "抱歉，获取天气预报时出现错误：" + e.getMessage();
        }
    }

    /**
     * 格式化当前天气响应
     */
    @SuppressWarnings("unchecked")
    private String formatCurrentWeatherResponse(Map<String, Object> response) {
        try {
            StringBuilder result = new StringBuilder();
            
            // 获取城市信息
            String cityName = response.get("name").toString();
            result.append("📍 城市：").append(cityName).append("\n");

            // 获取天气信息
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            if (main != null) {
                result.append("🌡️ 温度：").append(main.get("temp")).append("°C\n");
                result.append("🌡️ 体感温度：").append(main.get("feels_like")).append("°C\n");
                result.append("💧 湿度：").append(main.get("humidity")).append("%\n");
                result.append("📊 气压：").append(main.get("pressure")).append(" hPa\n");
            }

            // 获取天气描述
            List<Map<String, Object>> weather = (List<Map<String, Object>>) response.get("weather");
            if (weather != null && !weather.isEmpty()) {
                Map<String, Object> weatherInfo = weather.get(0);
                result.append("☁️ 天气：").append(weatherInfo.get("description")).append("\n");
            }

            // 获取风速
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");
            if (wind != null) {
                result.append("💨 风速：").append(wind.get("speed")).append(" m/s\n");
            }

            // 获取能见度
            if (response.containsKey("visibility")) {
                result.append("👁️ 能见度：").append(response.get("visibility")).append(" m\n");
            }

            return result.toString();

        } catch (Exception e) {
            logger.error("格式化天气响应失败: {}", e.getMessage());
            return "天气信息解析失败";
        }
    }

    /**
     * 格式化天气预报响应
     */
    @SuppressWarnings("unchecked")
    private String formatForecastResponse(Map<String, Object> response) {
        try {
            StringBuilder result = new StringBuilder();
            
            String cityName = response.get("city").toString();
            result.append("📍 城市：").append(cityName).append("\n\n");

            List<Map<String, Object>> forecastList = (List<Map<String, Object>>) response.get("list");
            if (forecastList != null) {
                result.append("📅 未来5天天气预报：\n");
                
                // 只显示前5个预报（大约40小时）
                int count = Math.min(5, forecastList.size());
                for (int i = 0; i < count; i++) {
                    Map<String, Object> forecast = forecastList.get(i);
                    
                    // 获取时间
                    String dtTxt = forecast.get("dt_txt").toString();
                    result.append("\n🕐 ").append(dtTxt).append("\n");
                    
                    // 获取温度信息
                    Map<String, Object> main = (Map<String, Object>) forecast.get("main");
                    if (main != null) {
                        result.append("🌡️ 温度：").append(main.get("temp")).append("°C\n");
                        result.append("💧 湿度：").append(main.get("humidity")).append("%\n");
                    }
                    
                    // 获取天气描述
                    List<Map<String, Object>> weather = (List<Map<String, Object>>) forecast.get("weather");
                    if (weather != null && !weather.isEmpty()) {
                        Map<String, Object> weatherInfo = weather.get(0);
                        result.append("☁️ 天气：").append(weatherInfo.get("description")).append("\n");
                    }
                    
                    // 获取风速
                    Map<String, Object> wind = (Map<String, Object>) forecast.get("wind");
                    if (wind != null) {
                        result.append("💨 风速：").append(wind.get("speed")).append(" m/s\n");
                    }
                }
            }

            return result.toString();

        } catch (Exception e) {
            logger.error("格式化天气预报响应失败: {}", e.getMessage());
            return "天气预报信息解析失败";
        }
    }

    /**
     * 获取工具描述信息
     */
    public String getToolDescription() {
        return "天气查询工具 - 可以查询指定城市的当前天气和天气预报信息";
    }

    /**
     * 获取支持的功能列表
     */
    public List<String> getSupportedFunctions() {
        List<String> functions = new ArrayList<>();
        functions.add("getCurrentWeather(city) - 获取当前天气");
        functions.add("getWeatherForecast(city) - 获取天气预报");
        return functions;
    }

    /**
     * 检查工具是否可用
     */
    public boolean isToolAvailable() {
        try {
            // 尝试查询一个测试城市的天气
            String testResult = getCurrentWeather("Beijing");
            return testResult != null && !testResult.contains("错误") && !testResult.contains("失败");
        } catch (Exception e) {
            logger.warn("天气工具可用性检查失败: {}", e.getMessage());
            return false;
        }
    }
}
