package com.example.qa.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * SpringAIService测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.alibaba.api-key=test-key",
    "spring.ai.alibaba.endpoint=https://dashscope.aliyuncs.com/api/v1",
    "spring.ai.alibaba.model=qwen-turbo"
})
class SpringAIServiceTest {

    @Autowired
    private SpringAIService springAIService;

    @Test
    void testGetSupportedModels() {
        List<String> models = springAIService.getSupportedModels();
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("qwen-turbo"));
        assertTrue(models.contains("qwen-plus"));
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(springAIService);
    }
}
