package com.example.qa.service;

import com.example.qa.entity.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 材料上下文服务
 * 用于为材料相关问题提供数据库中的文档作为上下文
 */
@Service
public class MaterialsContextService {

    @Autowired
    private DocumentService documentService;

    /**
     * 获取所有文档作为材料上下文
     * @return 格式化的文档内容字符串
     */
    public String getAllDocumentsAsContext() {
        try {
            List<Document> documents = documentService.getAllActiveDocuments();
            
            if (documents.isEmpty()) {
                return "当前数据库中没有材料文档。";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("以下是数据库中的所有材料文档，可以作为回答问题的参考：\n\n");
            
            for (Document doc : documents) {
                context.append("=== ").append(doc.getTitle()).append(" ===\n");
                if (doc.getCategory() != null && !doc.getCategory().isEmpty()) {
                    context.append("分类: ").append(doc.getCategory()).append("\n");
                }
                if (doc.getAuthor() != null && !doc.getAuthor().isEmpty()) {
                    context.append("作者: ").append(doc.getAuthor()).append("\n");
                }
                context.append("内容:\n").append(doc.getContent()).append("\n\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            return "获取材料文档时发生错误：" + e.getMessage();
        }
    }

    /**
     * 根据关键词搜索相关文档作为上下文
     * @param keyword 搜索关键词
     * @return 格式化的相关文档内容字符串
     */
    public String getRelevantDocumentsAsContext(String keyword) {
        try {
            List<Document> documents = documentService.searchByKeyword(keyword);
            
            if (documents.isEmpty()) {
                return "没有找到与关键词 '" + keyword + "' 相关的材料文档。";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("以下是与关键词 '").append(keyword).append("' 相关的材料文档：\n\n");
            
            for (Document doc : documents) {
                context.append("=== ").append(doc.getTitle()).append(" ===\n");
                if (doc.getCategory() != null && !doc.getCategory().isEmpty()) {
                    context.append("分类: ").append(doc.getCategory()).append("\n");
                }
                context.append("内容:\n").append(doc.getContent()).append("\n\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            return "搜索相关材料文档时发生错误：" + e.getMessage();
        }
    }

    /**
     * 根据分类获取文档作为上下文
     * @param category 文档分类
     * @return 格式化的分类文档内容字符串
     */
    public String getDocumentsByCategoryAsContext(String category) {
        try {
            List<Document> documents = documentService.getDocumentsByCategory(category);
            
            if (documents.isEmpty()) {
                return "没有找到分类为 '" + category + "' 的材料文档。";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("以下是分类为 '").append(category).append("' 的材料文档：\n\n");
            
            for (Document doc : documents) {
                context.append("=== ").append(doc.getTitle()).append(" ===\n");
                context.append("内容:\n").append(doc.getContent()).append("\n\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            return "获取分类材料文档时发生错误：" + e.getMessage();
        }
    }

    /**
     * 获取材料文档统计信息
     * @return 统计信息字符串
     */
    public String getMaterialsStatistics() {
        try {
            long totalCount = documentService.getTotalDocumentCount();
            long activeCount = documentService.getActiveDocumentCount();
            
            StringBuilder stats = new StringBuilder();
            stats.append("材料文档统计信息：\n");
            stats.append("- 总文档数: ").append(totalCount).append("\n");
            stats.append("- 活跃文档数: ").append(activeCount).append("\n");
            
            List<Object[]> categoryStats = documentService.getCategoryStatistics();
            if (!categoryStats.isEmpty()) {
                stats.append("- 分类统计:\n");
                for (Object[] stat : categoryStats) {
                    stats.append("  * ").append(stat[0]).append(": ").append(stat[1]).append(" 个文档\n");
                }
            }
            
            return stats.toString();
            
        } catch (Exception e) {
            return "获取材料文档统计信息时发生错误：" + e.getMessage();
        }
    }
}
