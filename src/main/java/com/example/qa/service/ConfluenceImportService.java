package com.example.qa.service;

import com.example.qa.entity.Document;
import com.example.qa.repository.DocumentRepository;
import com.example.qa.tool.ConfluenceTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Confluence文档导入服务
 * 负责将Confluence文档导入到SQLite数据库中
 */
@Service
@Transactional
public class ConfluenceImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceImportService.class);
    
    @Autowired
    private ConfluenceTool confluenceTool;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    /**
     * 导入指定时间范围内的Confluence文档
     * @param days 天数
     * @return 导入结果
     */
    public ImportResult importDocumentsFromConfluence(int days) {
        try {
            logger.info("开始导入Confluence文档，时间范围：过去{}天", days);
            
            // 获取Confluence更新内容
            String updatesContent = confluenceTool.getUpdatesForDays(days);
            
            // 解析Confluence内容并提取文档信息
            List<ConfluencePageInfo> pageInfos = parseConfluenceContent(updatesContent);
            
            if (pageInfos.isEmpty()) {
                return new ImportResult(0, 0, "没有找到可导入的文档");
            }
            
            // 导入文档到数据库
            int successCount = 0;
            int skipCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (ConfluencePageInfo pageInfo : pageInfos) {
                try {
                    // 检查文档是否已存在
                    if (documentService.documentExistsByTitleAndAuthor(pageInfo.getTitle(), pageInfo.getAuthor())) {
                        skipCount++;
                        logger.info("文档已存在，跳过：{}", pageInfo.getTitle());
                        continue;
                    }
                    
                    // 创建文档实体
                    Document document = createDocumentFromPageInfo(pageInfo);
                    
                    // 保存到数据库
                    Document savedDocument = documentService.createDocument(document);
                    successCount++;
                    
                    logger.info("成功导入文档：{} (ID: {})", savedDocument.getTitle(), savedDocument.getId());
                    
                } catch (Exception e) {
                    String error = "导入文档失败：" + pageInfo.getTitle() + " - " + e.getMessage();
                    errors.add(error);
                    logger.error(error, e);
                }
            }
            
            String message = String.format("导入完成。成功：%d，跳过：%d，失败：%d", 
                successCount, skipCount, errors.size());
            
            return new ImportResult(successCount, skipCount, message, errors);
            
        } catch (Exception e) {
            logger.error("Confluence文档导入失败", e);
            return new ImportResult(0, 0, "导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 导入所有Confluence文档（不限制时间）
     */
    public ImportResult importAllDocumentsFromConfluence() {
        try {
            logger.info("开始导入所有Confluence文档");
            
            // 获取所有页面
            List<Map<String, Object>> allPages = getAllConfluencePages();
            
            if (allPages.isEmpty()) {
                return new ImportResult(0, 0, "没有找到可导入的文档");
            }
            
            int successCount = 0;
            int skipCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map<String, Object> page : allPages) {
                try {
                    String pageId = page.get("id").toString();
                    String pageTitle = page.get("title").toString();
                    
                    // 检查文档是否已存在
                    if (documentService.documentExistsByTitleAndAuthor(pageTitle, "Confluence")) {
                        skipCount++;
                        logger.info("文档已存在，跳过：{}", pageTitle);
                        continue;
                    }
                    
                    // 获取页面内容
                    String content = confluenceTool.getPageContent(pageId);
                    if (content == null || content.trim().isEmpty()) {
                        skipCount++;
                        logger.info("页面内容为空，跳过：{}", pageTitle);
                        continue;
                    }
                    
                    // 创建文档实体
                    Document document = new Document();
                    document.setTitle(pageTitle);
                    document.setContent(content);
                    document.setAuthor("Confluence");
                    document.setCategory("Confluence导入");
                    document.setTags("confluence,导入,文档");
                    document.setFilePath("confluence://" + pageId);
                    document.setMimeType("text/html");
                    document.setFileSize((long) content.length());
                    
                    // 保存到数据库
                    Document savedDocument = documentService.createDocument(document);
                    successCount++;
                    
                    logger.info("成功导入文档：{} (ID: {})", savedDocument.getTitle(), savedDocument.getId());
                    
                } catch (Exception e) {
                    String error = "导入文档失败：" + page.get("title") + " - " + e.getMessage();
                    errors.add(error);
                    logger.error(error, e);
                }
            }
            
            String message = String.format("导入完成。成功：%d，跳过：%d，失败：%d", 
                successCount, skipCount, errors.size());
            
            return new ImportResult(successCount, skipCount, message, errors);
            
        } catch (Exception e) {
            logger.error("Confluence文档导入失败", e);
            return new ImportResult(0, 0, "导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 导入指定页面ID的文档
     */
    public ImportResult importDocumentByPageId(String pageId) {
        try {
            logger.info("开始导入指定页面文档，页面ID：{}", pageId);
            
            // 获取页面内容
            String content = confluenceTool.getPageContent(pageId);
            if (content == null || content.trim().isEmpty()) {
                return new ImportResult(0, 0, "页面内容为空或无法获取");
            }
            
            // 获取页面基本信息
            Map<String, Object> pageInfo = getPageInfo(pageId);
            if (pageInfo == null) {
                return new ImportResult(0, 0, "无法获取页面信息");
            }
            
            String pageTitle = pageInfo.get("title").toString();
            
            // 检查文档是否已存在
            if (documentService.documentExistsByTitleAndAuthor(pageTitle, "Confluence")) {
                return new ImportResult(0, 1, "文档已存在：" + pageTitle);
            }
            
            // 创建文档实体
            Document document = new Document();
            document.setTitle(pageTitle);
            document.setContent(content);
            document.setAuthor("Confluence");
            document.setCategory("Confluence导入");
            document.setTags("confluence,导入,文档");
            document.setFilePath("confluence://" + pageId);
            document.setMimeType("text/html");
            document.setFileSize((long) content.length());
            
            // 保存到数据库
            Document savedDocument = documentService.createDocument(document);
            
            String message = "成功导入文档：" + savedDocument.getTitle();
            return new ImportResult(1, 0, message);
            
        } catch (Exception e) {
            logger.error("导入指定页面文档失败，页面ID：{}", pageId, e);
            return new ImportResult(0, 0, "导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析Confluence内容，提取页面信息
     */
    private List<ConfluencePageInfo> parseConfluenceContent(String content) {
        List<ConfluencePageInfo> pageInfos = new ArrayList<>();
        
        try {
            // 这里可以根据实际的Confluence内容格式进行解析
            // 目前简化处理，假设内容包含页面标题和内容
            
            String[] sections = content.split("---");
            for (String section : sections) {
                if (section.contains("标题:") && section.contains("内容:")) {
                    String[] lines = section.split("\n");
                    String title = "";
                    StringBuilder contentBuilder = new StringBuilder();
                    
                    for (String line : lines) {
                        if (line.startsWith("标题:")) {
                            title = line.substring(3).trim();
                        } else if (line.startsWith("内容:")) {
                            contentBuilder.append(line.substring(3).trim());
                        } else if (!line.trim().isEmpty()) {
                            contentBuilder.append(" ").append(line.trim());
                        }
                    }
                    
                    if (!title.isEmpty() && contentBuilder.length() > 0) {
                        ConfluencePageInfo pageInfo = new ConfluencePageInfo();
                        pageInfo.setTitle(title);
                        pageInfo.setContent(contentBuilder.toString());
                        pageInfo.setAuthor("Confluence");
                        pageInfo.setCategory("Confluence导入");
                        pageInfo.setTags("confluence,导入,文档");
                        pageInfos.add(pageInfo);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("解析Confluence内容失败", e);
        }
        
        return pageInfos;
    }
    
    /**
     * 从页面信息创建文档实体
     */
    private Document createDocumentFromPageInfo(ConfluencePageInfo pageInfo) {
        Document document = new Document();
        document.setTitle(pageInfo.getTitle());
        document.setContent(pageInfo.getContent());
        document.setAuthor(pageInfo.getAuthor());
        document.setCategory(pageInfo.getCategory());
        document.setTags(pageInfo.getTags());
        document.setFilePath("confluence://imported");
        document.setMimeType("text/html");
        document.setFileSize((long) pageInfo.getContent().length());
        return document;
    }
    
    /**
     * 获取所有Confluence页面
     */
    private List<Map<String, Object>> getAllConfluencePages() {
        try {
            return confluenceTool.getAllPages();
        } catch (Exception e) {
            logger.error("获取所有Confluence页面失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取页面基本信息
     */
    private Map<String, Object> getPageInfo(String pageId) {
        try {
            return confluenceTool.getPageInfo(pageId);
        } catch (Exception e) {
            logger.error("获取页面信息失败，页面ID：{}", pageId, e);
            return null;
        }
    }
    
    /**
     * 导入结果类
     */
    public static class ImportResult {
        private int successCount;
        private int skipCount;
        private String message;
        private List<String> errors;
        
        public ImportResult(int successCount, int skipCount, String message) {
            this.successCount = successCount;
            this.skipCount = skipCount;
            this.message = message;
            this.errors = new ArrayList<>();
        }
        
        public ImportResult(int successCount, int skipCount, String message, List<String> errors) {
            this.successCount = successCount;
            this.skipCount = skipCount;
            this.message = message;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        // Getters
        public int getSuccessCount() { return successCount; }
        public int getSkipCount() { return skipCount; }
        public String getMessage() { return message; }
        public List<String> getErrors() { return errors; }
        public int getTotalProcessed() { return successCount + skipCount + errors.size(); }
    }
    
    /**
     * Confluence页面信息类
     */
    public static class ConfluencePageInfo {
        private String title;
        private String content;
        private String author;
        private String category;
        private String tags;
        
        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }
}
