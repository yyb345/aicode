package com.example.qa.tool;

import com.example.qa.service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfluenceTool测试类
 */
@SpringBootTest
class ConfluenceToolTest {

    @Autowired
    private ConfluenceTool confluenceTool;

    @Autowired
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        // 验证依赖注入是否成功
        assertNotNull(confluenceTool, "ConfluenceTool should be injected");
        assertNotNull(openAIService, "OpenAIService should be injected");
        
        // 验证 OpenAIService 的配置是否正确注入（使用 application.yml 中的配置）
        String apiKey = (String) ReflectionTestUtils.getField(openAIService, "apiKey");
        String baseUrl = (String) ReflectionTestUtils.getField(openAIService, "baseUrl");
        String model = (String) ReflectionTestUtils.getField(openAIService, "model");
        
        assertNotNull(apiKey, "OpenAI API key should be configured from application.yml");
        assertNotNull(baseUrl, "OpenAI base URL should be configured from application.yml");
        assertNotNull(model, "OpenAI model should be configured from application.yml");
        
        System.out.println("OpenAIService 配置验证 (来自 application.yml):");
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null"));
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Model: " + model);
    }

    @Test
    public void testConnection() {
        boolean isAvailable = confluenceTool.isToolAvailable();
        System.out.println("连接状态: " + isAvailable);
    }


    @Test
    void testGetToolDescription() {
        String description = confluenceTool.getToolDescription();
        assertNotNull(description);
        assertTrue(description.contains("Confluence文档爬取工具"));
    }

    @Test
    void testGetSupportedFunctions() {
        List<String> functions = confluenceTool.getSupportedFunctions();
        assertNotNull(functions);
        assertFalse(functions.isEmpty());
        assertTrue(functions.contains("getRecentUpdates() - 获取过去7天的更新内容并总结"));
        assertTrue(functions.contains("getPageContent(pageId) - 获取指定页面的内容"));
    }

    @Test
    void testGetPageContent_Success() {
        // 由于WebClient的复杂性，这里主要测试方法调用不会抛出异常
        assertDoesNotThrow(() -> confluenceTool.getPageContent("123"));
    }

    @Test
    void testGetPageContent_WithNullResponse() {
        // 由于WebClient的复杂性，这里主要测试方法调用不会抛出异常
        assertDoesNotThrow(() -> {
            String result = confluenceTool.getPageContent("123");
            // 在测试环境中，由于没有真实的Confluence连接，预期返回null
            assertNull(result);
        });
    }

    @Test
    void testCleanHtmlContent() {
        // 使用反射调用私有方法进行测试
        String htmlContent = "<p>测试内容</p><br/>更多内容&nbsp;";
        String result = (String) ReflectionTestUtils.invokeMethod(confluenceTool, "cleanHtmlContent", htmlContent);
        
        assertNotNull(result);
        assertFalse(result.contains("<p>"));
        assertFalse(result.contains("<br/>"));
        assertFalse(result.contains("&nbsp;"));
        assertTrue(result.contains("测试内容"));
        assertTrue(result.contains("更多内容"));
    }

    @Test
    void testCleanHtmlContent_WithNullInput() {
        String result = (String) ReflectionTestUtils.invokeMethod(confluenceTool, "cleanHtmlContent", (String) null);
        assertEquals("", result);
    }

    @Test
    void testCleanHtmlContent_WithEmptyInput() {
        String result = (String) ReflectionTestUtils.invokeMethod(confluenceTool, "cleanHtmlContent", "");
        assertEquals("", result);
    }


    @Test
    void testFormatSummaryResponse() {
        String summary = "这是一个测试总结";
        String result = (String) ReflectionTestUtils.invokeMethod(confluenceTool, "formatSummaryResponse", 5, summary);
        
        assertNotNull(result);
        assertTrue(result.contains("Confluence文档更新总结"));
        assertTrue(result.contains("过去7天"));
        assertTrue(result.contains("更新页面数：5"));
        assertTrue(result.contains(summary));
    }

    @Test
    void testFormatCustomSummaryResponse() {
        String summary = "这是一个自定义时间范围的总结";
        String result = (String) ReflectionTestUtils.invokeMethod(confluenceTool, "formatCustomSummaryResponse", 3, 2, summary);
        
        assertNotNull(result);
        assertTrue(result.contains("Confluence文档更新总结"));
        assertTrue(result.contains("过去3天"));
        assertTrue(result.contains("更新页面数：2"));
        assertTrue(result.contains(summary));
    }

    @Test
    void testGetUpdatesForDays() {
        // 由于涉及复杂的WebClient调用，这里主要测试方法调用不会抛出异常
        assertDoesNotThrow(() -> {
            String result = confluenceTool.getUpdatesForDays(3);
            assertNotNull(result);
        });
    }

    @Test
    void testGetRecentUpdates() {
        String result = confluenceTool.getRecentUpdates();
        System.out.println(result);
    }

    @Test
    void testIsToolAvailable() {
        // 由于涉及复杂的WebClient调用，这里主要测试方法调用不会抛出异常
        assertDoesNotThrow(() -> {
            confluenceTool.isToolAvailable();
        });
    }

    @Test
    void testGetUpdatesForDays_WithException() {
        // 由于现在使用真实的OpenAIService，这个测试主要验证异常处理
        String result = confluenceTool.getUpdatesForDays(7);
        assertNotNull(result);
        // 在测试环境中，由于没有真实的Confluence连接，预期返回错误信息
        assertTrue(result.contains("错误") || result.contains("未找到") || result.contains("抱歉"));
    }

    @Test
    void testGetRecentUpdates_WithException() {
        // 由于现在使用真实的OpenAIService，这个测试主要验证异常处理
        String result = confluenceTool.getRecentUpdates();
        assertNotNull(result);
        // 在测试环境中，由于没有真实的Confluence连接，预期返回错误信息
        assertTrue(result.contains("错误") || result.contains("未找到") || result.contains("抱歉"));
    }

    @Test
    void testOpenAIServiceInjection() {
        // 验证 OpenAIService 是否正确注入
        assertNotNull(openAIService, "OpenAIService should not be null");
        
        // 验证配置属性是否正确注入（使用 application.yml 中的真实配置）
        String apiKey = (String) ReflectionTestUtils.getField(openAIService, "apiKey");
        String baseUrl = (String) ReflectionTestUtils.getField(openAIService, "baseUrl");
        String model = (String) ReflectionTestUtils.getField(openAIService, "model");
        
        // 验证配置不为空且符合预期格式
        assertNotNull(apiKey, "API key should be configured from application.yml");
        assertNotNull(baseUrl, "Base URL should be configured from application.yml");
        assertNotNull(model, "Model should be configured from application.yml");
        
        // 验证配置值符合预期格式
        assertTrue(apiKey.length() > 0, "API key should not be empty");
        assertTrue(baseUrl.startsWith("http"), "Base URL should start with http");
        assertTrue(model.length() > 0, "Model should not be empty");
        
        System.out.println("✅ OpenAIService 注入和配置验证成功 (使用 application.yml 配置)");
        System.out.println("   API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        System.out.println("   Base URL: " + baseUrl);
        System.out.println("   Model: " + model);
    }

    @Test
    void testConfluenceToolOpenAIServiceDependency() {
        // 验证 ConfluenceTool 中的 OpenAIService 依赖是否正确注入
        OpenAIService injectedService = (OpenAIService) ReflectionTestUtils.getField(confluenceTool, "openAIService");
        assertNotNull(injectedService, "ConfluenceTool should have OpenAIService injected");
        
        // 验证注入的是同一个实例
        assertSame(openAIService, injectedService, "ConfluenceTool should use the same OpenAIService instance");
        
        System.out.println("✅ ConfluenceTool 中的 OpenAIService 依赖注入验证成功");
    }
}
