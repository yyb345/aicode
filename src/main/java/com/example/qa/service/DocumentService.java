package com.example.qa.service;

import com.example.qa.entity.Document;
import com.example.qa.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档服务层
 * 提供文档的业务逻辑处理
 */
@Service
@Transactional
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    /**
     * 创建新文档
     */
    public Document createDocument(Document document) {
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setIsActive(true);
        return documentRepository.save(document);
    }
    
    /**
     * 根据ID获取文档
     */
    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }
    
    /**
     * 获取所有活跃的文档
     */
    @Transactional(readOnly = true)
    public List<Document> getAllActiveDocuments() {
        return documentRepository.findByIsActiveTrue();
    }
    
    /**
     * 分页获取活跃文档
     */
    @Transactional(readOnly = true)
    public Page<Document> getActiveDocuments(Pageable pageable) {
        return documentRepository.findByIsActiveTrue(pageable);
    }
    
    /**
     * 根据标题搜索文档
     */
    @Transactional(readOnly = true)
    public List<Document> searchByTitle(String title) {
        return documentRepository.findByTitleContainingIgnoreCase(title);
    }
    
    /**
     * 根据作者获取文档
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByAuthor(String author) {
        return documentRepository.findByAuthor(author);
    }
    
    /**
     * 根据分类获取文档
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByCategory(String category) {
        return documentRepository.findByCategory(category);
    }
    
    /**
     * 根据分类分页获取文档
     */
    @Transactional(readOnly = true)
    public Page<Document> getDocumentsByCategory(String category, Pageable pageable) {
        return documentRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }
    
    /**
     * 根据标签搜索文档
     */
    @Transactional(readOnly = true)
    public List<Document> searchByTag(String tag) {
        return documentRepository.findByTagContaining(tag);
    }
    
    /**
     * 根据内容关键词搜索文档
     */
    @Transactional(readOnly = true)
    public List<Document> searchByContent(String keyword) {
        return documentRepository.findByContentContaining(keyword);
    }
    
    /**
     * 综合搜索文档
     */
    @Transactional(readOnly = true)
    public List<Document> searchByKeyword(String keyword) {
        return documentRepository.findByKeyword(keyword);
    }
    
    /**
     * 根据创建时间范围获取文档
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return documentRepository.findByCreatedAtBetween(startDate, endDate);
    }
    
    /**
     * 更新文档
     */
    public Document updateDocument(Long id, Document updatedDocument) {
        Optional<Document> existingDocument = documentRepository.findById(id);
        if (existingDocument.isPresent()) {
            Document document = existingDocument.get();
            
            // 更新字段
            if (updatedDocument.getTitle() != null) {
                document.setTitle(updatedDocument.getTitle());
            }
            if (updatedDocument.getContent() != null) {
                document.setContent(updatedDocument.getContent());
            }
            if (updatedDocument.getAuthor() != null) {
                document.setAuthor(updatedDocument.getAuthor());
            }
            if (updatedDocument.getCategory() != null) {
                document.setCategory(updatedDocument.getCategory());
            }
            if (updatedDocument.getTags() != null) {
                document.setTags(updatedDocument.getTags());
            }
            if (updatedDocument.getFilePath() != null) {
                document.setFilePath(updatedDocument.getFilePath());
            }
            if (updatedDocument.getFileSize() != null) {
                document.setFileSize(updatedDocument.getFileSize());
            }
            if (updatedDocument.getMimeType() != null) {
                document.setMimeType(updatedDocument.getMimeType());
            }
            
            // 设置更新时间
            document.setUpdatedAt(LocalDateTime.now());
            
            return documentRepository.save(document);
        }
        throw new RuntimeException("Document not found with id: " + id);
    }
    
    /**
     * 软删除文档
     */
    public boolean softDeleteDocument(Long id) {
        int updatedRows = documentRepository.softDeleteById(id, LocalDateTime.now());
        return updatedRows > 0;
    }
    
    /**
     * 恢复软删除的文档
     */
    public boolean restoreDocument(Long id) {
        int updatedRows = documentRepository.restoreById(id, LocalDateTime.now());
        return updatedRows > 0;
    }
    
    /**
     * 硬删除文档（物理删除）
     */
    public boolean deleteDocument(Long id) {
        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * 检查文档是否存在
     */
    @Transactional(readOnly = true)
    public boolean documentExists(Long id) {
        return documentRepository.existsById(id);
    }
    
    /**
     * 检查标题和作者组合是否存在
     */
    @Transactional(readOnly = true)
    public boolean documentExistsByTitleAndAuthor(String title, String author) {
        return documentRepository.findByTitleAndAuthor(title, author).isPresent();
    }
    
    /**
     * 获取文档统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getCategoryStatistics() {
        return documentRepository.countByCategory();
    }
    
    /**
     * 获取作者统计信息
     */
    @Transactional(readOnly = true)
    public List<Object[]> getAuthorStatistics() {
        return documentRepository.countByAuthor();
    }
    
    /**
     * 获取文档总数
     */
    @Transactional(readOnly = true)
    public long getTotalDocumentCount() {
        return documentRepository.count();
    }
    
    /**
     * 获取活跃文档总数
     */
    @Transactional(readOnly = true)
    public long getActiveDocumentCount() {
        return documentRepository.findByIsActiveTrue().size();
    }
}
