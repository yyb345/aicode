package com.example.qa.controller;

import com.example.qa.tool.WeatherTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WeatherController测试类
 */
@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherTool weatherTool;

    @Test
    void testGetCurrentWeather() throws Exception {
        // 模拟天气工具返回
        when(weatherTool.getCurrentWeather("北京")).thenReturn("📍 城市：北京\n🌡️ 温度：15°C");

        mockMvc.perform(get("/api/weather/current")
                .param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.city").value("北京"))
                .andExpect(jsonPath("$.weather").value("📍 城市：北京\n🌡️ 温度：15°C"));
    }

    @Test
    void testGetWeatherForecast() throws Exception {
        // 模拟天气预报工具返回
        when(weatherTool.getWeatherForecast("上海")).thenReturn("📍 城市：上海\n📅 未来5天天气预报");

        mockMvc.perform(get("/api/weather/forecast")
                .param("city", "上海"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.city").value("上海"))
                .andExpect(jsonPath("$.forecast").value("📍 城市：上海\n📅 未来5天天气预报"));
    }

    @Test
    void testSmartWeatherQuery() throws Exception {
        // 模拟智能天气查询
        when(weatherTool.getCurrentWeather("北京")).thenReturn("📍 城市：北京\n🌡️ 温度：15°C");

        mockMvc.perform(post("/api/weather/query")
                .contentType("application/json")
                .content("{\"question\": \"北京今天天气怎么样？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.city").value("北京"))
                .andExpect(jsonPath("$.queryType").value("current"));
    }

    @Test
    void testGetToolInfo() throws Exception {
        // 模拟工具信息
        when(weatherTool.getToolDescription()).thenReturn("天气查询工具");
        when(weatherTool.getSupportedFunctions()).thenReturn(java.util.Arrays.asList("getCurrentWeather", "getWeatherForecast"));
        when(weatherTool.isToolAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/weather/tool/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.weather_tool.description").value("天气查询工具"))
                .andExpect(jsonPath("$.weather_tool.available").value(true));
    }

    @Test
    void testGetToolStatus() throws Exception {
        // 模拟工具状态
        when(weatherTool.isToolAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/weather/tool/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("天气工具正常"));
    }
}
