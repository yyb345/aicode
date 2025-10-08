package com.example.qa.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * WeatherTool测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "weather.api.key=test-key"
})
class WeatherToolTest {

    @Autowired
    private WeatherTool weatherTool;

    @Test
    void testGetToolDescription() {
        String description = weatherTool.getToolDescription();
        assertNotNull(description);
        assertTrue(description.contains("天气查询"));
    }

    @Test
    void testGetSupportedFunctions() {
        List<String> functions = weatherTool.getSupportedFunctions();
        assertNotNull(functions);
        assertFalse(functions.isEmpty());
        assertTrue(functions.size() >= 2);
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(weatherTool);
    }

    @Test
    void testGetCurrentWeatherWithInvalidCity() {
        // 测试无效城市名称
        String result = weatherTool.getCurrentWeather("InvalidCity12345");
        assertNotNull(result);
        // 应该返回错误信息或默认信息
        assertTrue(result.contains("抱歉") || result.contains("无法获取"));
    }
}
