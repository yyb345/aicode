package com.example.qa.service;

import com.example.qa.entity.Document;
import com.example.qa.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Materials导入服务
 * 用于将materials.txt文件内容导入到SQLite数据库
 * 可以通过CommandLineRunner在应用启动时自动执行
 */
@Component
public class MaterialsImportService implements CommandLineRunner {

    @Autowired
    private DocumentService documentService;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否需要导入materials.txt
        if (args.length > 0 && "import-materials".equals(args[0])) {
            importMaterialsFromFile();
        }
    }

    /**
     * 手动导入materials.txt内容到数据库
     */
    public List<Document> importMaterialsFromFile() {
        try {
            System.out.println("开始导入materials.txt内容到数据库...");
            
            // 清空现有数据（可选）
            // documentRepository.deleteAll();
            
            List<Document> documents = parseMaterialsFile();
            
            // 保存到数据库
            List<Document> savedDocuments = new ArrayList<>();
            for (Document doc : documents) {
                Document savedDoc = documentService.createDocument(doc);
                savedDocuments.add(savedDoc);
            }
            
            System.out.println("成功导入 " + savedDocuments.size() + " 个文档到数据库");
            
            // 打印导入的文档信息
            for (Document doc : savedDocuments) {
                System.out.println("文档ID: " + doc.getId() + 
                                 ", 标题: " + doc.getTitle() + 
                                 ", 分类: " + doc.getCategory());
            }
            
            return savedDocuments;
            
        } catch (Exception e) {
            System.err.println("导入materials.txt时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 解析materials.txt文件
     */
    private List<Document> parseMaterialsFile() throws Exception {
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
                        documents.add(doc);
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
                        documents.add(doc);
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
                documents.add(doc);
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
     * 获取导入的文档统计信息
     */
    public void printImportStatistics() {
        long totalCount = documentService.getTotalDocumentCount();
        long activeCount = documentService.getActiveDocumentCount();
        
        System.out.println("=== 文档统计信息 ===");
        System.out.println("总文档数: " + totalCount);
        System.out.println("活跃文档数: " + activeCount);
        
        // 分类统计
        List<Object[]> categoryStats = documentService.getCategoryStatistics();
        if (!categoryStats.isEmpty()) {
            System.out.println("分类统计:");
            for (Object[] stat : categoryStats) {
                System.out.println("  " + stat[0] + ": " + stat[1] + " 个文档");
            }
        }
        
        // 作者统计
        List<Object[]> authorStats = documentService.getAuthorStatistics();
        if (!authorStats.isEmpty()) {
            System.out.println("作者统计:");
            for (Object[] stat : authorStats) {
                System.out.println("  " + stat[0] + ": " + stat[1] + " 个文档");
            }
        }
    }
}
