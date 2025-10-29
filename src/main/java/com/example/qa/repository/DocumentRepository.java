package com.example.qa.repository;

import com.example.qa.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档数据访问层
 * 提供文档的数据库操作接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * 根据标题查找文档
     */
    List<Document> findByTitleContainingIgnoreCase(String title);
    
    /**
     * 根据作者查找文档
     */
    List<Document> findByAuthor(String author);
    
    /**
     * 根据分类查找文档
     */
    List<Document> findByCategory(String category);
    
    /**
     * 查找活跃的文档
     */
    List<Document> findByIsActiveTrue();
    
    /**
     * 根据标题和作者查找文档
     */
    Optional<Document> findByTitleAndAuthor(String title, String author);
    
    /**
     * 根据创建时间范围查找文档
     */
    List<Document> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 根据标签查找文档（模糊匹配）
     */
    @Query("SELECT d FROM Document d WHERE d.tags LIKE %:tag% AND d.isActive = true")
    List<Document> findByTagContaining(@Param("tag") String tag);
    
    /**
     * 根据内容关键词搜索文档
     */
    @Query("SELECT d FROM Document d WHERE d.content LIKE %:keyword% AND d.isActive = true")
    List<Document> findByContentContaining(@Param("keyword") String keyword);
    
    /**
     * 综合搜索：标题、内容、标签
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(d.title LIKE %:keyword% OR d.content LIKE %:keyword% OR d.tags LIKE %:keyword%) " +
           "AND d.isActive = true")
    List<Document> findByKeyword(@Param("keyword") String keyword);
    
    /**
     * 分页查询活跃文档
     */
    Page<Document> findByIsActiveTrue(Pageable pageable);
    
    /**
     * 根据分类分页查询
     */
    Page<Document> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    /**
     * 统计各分类的文档数量
     */
    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.isActive = true GROUP BY d.category")
    List<Object[]> countByCategory();
    
    /**
     * 统计各作者的文档数量
     */
    @Query("SELECT d.author, COUNT(d) FROM Document d WHERE d.isActive = true GROUP BY d.author")
    List<Object[]> countByAuthor();
    
    /**
     * 软删除：将文档标记为非活跃状态
     */
    @Query("UPDATE Document d SET d.isActive = false, d.updatedAt = :updatedAt WHERE d.id = :id")
    int softDeleteById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);
    
    /**
     * 恢复软删除的文档
     */
    @Query("UPDATE Document d SET d.isActive = true, d.updatedAt = :updatedAt WHERE d.id = :id")
    int restoreById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);
}
