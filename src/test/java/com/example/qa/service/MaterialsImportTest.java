package com.example.qa.service;

import com.example.qa.entity.Document;
import com.example.qa.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Materials导入测试类
 * 用于测试将materials.txt文件内容导入到SQLite数据库
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MaterialsImportTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentService documentService;

    /**
     * 测试将materials.txt内容导入到数据库
     */
    @Test
    public void testImportMaterialsToDatabase() throws IOException {
        // 清空现有数据
        documentRepository.deleteAll();
        
        // 导入materials.txt内容
        List<Document> importedDocuments = importMaterialsFromFile();
        
        // 验证导入结果
        assertNotNull(importedDocuments);
        assertFalse(importedDocuments.isEmpty());
        
        // 验证数据库中的文档数量
        long totalCount = documentRepository.count();
        assertEquals(importedDocuments.size(), totalCount);
        
        // 验证每个文档都已正确保存
        for (Document doc : importedDocuments) {
            assertNotNull(doc.getId());
            assertNotNull(doc.getTitle());
            assertNotNull(doc.getContent());
            assertTrue(doc.getIsActive());
            assertNotNull(doc.getCreatedAt());
        }
        
        System.out.println("成功导入 " + importedDocuments.size() + " 个文档到数据库");
        
        // 打印导入的文档信息
        for (Document doc : importedDocuments) {
            System.out.println("文档ID: " + doc.getId() + 
                             ", 标题: " + doc.getTitle() + 
                             ", 分类: " + doc.getCategory());
        }
    }

    /**
     * 从materials.txt文件导入内容到数据库
     */
    public List<Document> importMaterialsFromFile() throws IOException {
        List<Document> documents = new ArrayList<>();
        
        // 读取materials.txt文件
        ClassPathResource resource = new ClassPathResource("materials.txt");
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder currentContent = new StringBuilder();
            String currentTitle = null;
            String currentCategory = null;
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }
                
                // 检查是否是新的分类标题（如"核心优势"、"数据&模型"等）
                if (isCategoryTitle(line)) {
                    // 保存前一个文档
                    if (currentTitle != null && currentContent.length() > 0) {
                        Document doc = createDocument(currentTitle, currentContent.toString(), currentCategory);
                        documents.add(documentService.createDocument(doc));
                    }
                    
                    // 开始新的分类
                    currentCategory = line;
                    currentTitle = null;
                    currentContent = new StringBuilder();
                    continue;
                }
                
                // 检查是否是FAQ标题
                if (isFAQTitle(line)) {
                    // 保存前一个文档
                    if (currentTitle != null && currentContent.length() > 0) {
                        Document doc = createDocument(currentTitle, currentContent.toString(), currentCategory);
                        documents.add(documentService.createDocument(doc));
                    }
                    
                    // 开始新的FAQ
                    currentTitle = line;
                    currentContent = new StringBuilder();
                    continue;
                }
                
                // 添加内容到当前文档
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
            
            // 保存最后一个文档
            if (currentTitle != null && currentContent.length() > 0) {
                Document doc = createDocument(currentTitle, currentContent.toString(), currentCategory);
                documents.add(documentService.createDocument(doc));
            }
        }
        
        return documents;
    }

    /**
     * 判断是否是分类标题
     */
    private boolean isCategoryTitle(String line) {
        // 分类标题通常不包含FAQ-xxx格式，且比较简短
        return !isFAQTitle(line) && 
               line.length() < 20 && 
               !line.contains("？") && 
               !line.contains("?") &&
               !line.contains("：") &&
               !line.contains(":");
    }

    /**
     * 判断是否是FAQ标题
     */
    private boolean isFAQTitle(String line) {
        // FAQ标题通常以FAQ-xxx开头
        Pattern faqPattern = Pattern.compile("^FAQ-\\d+");
        return faqPattern.matcher(line).find();
    }

    /**
     * 创建文档对象
     */
    private Document createDocument(String title, String content, String category) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setCategory(category);
        document.setAuthor("System");
        document.setTags("materials,faq,import");
        document.setMimeType("text/plain");
        document.setCreatedAt(LocalDateTime.now());
        document.setIsActive(true);
        
        return document;
    }

    /**
     * 测试搜索导入的文档
     */
    @Test
    public void testSearchImportedDocuments() throws IOException {
        // 先导入数据
        testImportMaterialsToDatabase();
        
        // 测试按分类搜索
        List<Document> coreAdvantageDocs = documentService.getDocumentsByCategory("核心优势");
        assertFalse(coreAdvantageDocs.isEmpty());
        
        // 测试按关键词搜索
        List<Document> searchResults = documentService.searchByKeyword("材料");
        assertFalse(searchResults.isEmpty());
        
        // 测试按内容搜索
        List<Document> contentResults = documentService.searchByContent("Eureka");
        assertFalse(contentResults.isEmpty());
        
        System.out.println("搜索测试通过:");
        System.out.println("- 核心优势分类文档数量: " + coreAdvantageDocs.size());
        System.out.println("- 包含'材料'关键词的文档数量: " + searchResults.size());
        System.out.println("- 包含'Eureka'内容的文档数量: " + contentResults.size());
    }

    /**
     * 测试文档统计功能
     */
    @Test
    public void testDocumentStatistics() throws IOException {
        // 先导入数据
        testImportMaterialsToDatabase();
        
        // 测试总文档数
        long totalCount = documentService.getTotalDocumentCount();
        assertTrue(totalCount > 0);
        
        // 测试活跃文档数
        long activeCount = documentService.getActiveDocumentCount();
        assertEquals(totalCount, activeCount);
        
        // 测试分类统计
        List<Object[]> categoryStats = documentService.getCategoryStatistics();
        assertFalse(categoryStats.isEmpty());
        
        System.out.println("统计测试通过:");
        System.out.println("- 总文档数: " + totalCount);
        System.out.println("- 活跃文档数: " + activeCount);
        System.out.println("- 分类统计:");
        for (Object[] stat : categoryStats) {
            System.out.println("  " + stat[0] + ": " + stat[1] + " 个文档");
        }
    }

    /**
     * 测试文档更新功能
     */
    @Test
    public void testUpdateDocument() throws IOException {
        // 先导入数据
        testImportMaterialsToDatabase();
        
        // 获取第一个文档
        List<Document> documents = documentService.getAllActiveDocuments();
        assertFalse(documents.isEmpty());
        
        Document firstDoc = documents.get(0);
        Long docId = firstDoc.getId();
        
        // 更新文档
        Document updatedDoc = new Document();
        updatedDoc.setTitle("更新后的标题");
        updatedDoc.setContent("更新后的内容");
        updatedDoc.setAuthor("测试用户");
        
        Document result = documentService.updateDocument(docId, updatedDoc);
        
        // 验证更新结果
        assertNotNull(result);
        assertEquals("更新后的标题", result.getTitle());
        assertEquals("更新后的内容", result.getContent());
        assertEquals("测试用户", result.getAuthor());
        assertNotNull(result.getUpdatedAt());
        
        System.out.println("文档更新测试通过，文档ID: " + docId);
    }

    /**
     * 测试软删除功能
     */
    @Test
    public void testSoftDeleteDocument() throws IOException {
        // 先导入数据
        testImportMaterialsToDatabase();
        
        // 获取第一个文档
        List<Document> documents = documentService.getAllActiveDocuments();
        assertFalse(documents.isEmpty());
        
        Document firstDoc = documents.get(0);
        Long docId = firstDoc.getId();
        
        // 软删除文档
        boolean deleted = documentService.softDeleteDocument(docId);
        assertTrue(deleted);
        
        // 验证文档已被软删除
        long activeCountBefore = documentService.getActiveDocumentCount();
        long totalCount = documentService.getTotalDocumentCount();
        
        assertEquals(totalCount - 1, activeCountBefore);
        
        // 恢复文档
        boolean restored = documentService.restoreDocument(docId);
        assertTrue(restored);
        
        // 验证文档已恢复
        long activeCountAfter = documentService.getActiveDocumentCount();
        assertEquals(totalCount, activeCountAfter);
        
        System.out.println("软删除和恢复测试通过，文档ID: " + docId);
    }
}
