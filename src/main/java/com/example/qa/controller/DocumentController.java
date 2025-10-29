package com.example.qa.controller;

import com.example.qa.entity.Document;
import com.example.qa.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档控制器
 * 提供文档的REST API接口
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * 创建新文档
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDocument(@RequestBody Document document) {
        try {
            Document savedDocument = documentService.createDocument(document);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文档创建成功");
            response.put("data", savedDocument);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文档创建失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 根据ID获取文档
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentById(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        Map<String, Object> response = new HashMap<>();
        
        if (document.isPresent()) {
            response.put("success", true);
            response.put("message", "文档获取成功");
            response.put("data", document.get());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "文档不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * 获取所有活跃文档
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActiveDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Document> documents = documentService.getActiveDocuments(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文档列表获取成功");
            response.put("data", documents.getContent());
            response.put("totalElements", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("currentPage", documents.getNumber());
            response.put("pageSize", documents.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取文档列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据标题搜索文档
     */
    @GetMapping("/search/title")
    public ResponseEntity<Map<String, Object>> searchByTitle(@RequestParam String title) {
        try {
            List<Document> documents = documentService.searchByTitle(title);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "搜索完成");
            response.put("data", documents);
            response.put("count", documents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据作者获取文档
     */
    @GetMapping("/author/{author}")
    public ResponseEntity<Map<String, Object>> getDocumentsByAuthor(@PathVariable String author) {
        try {
            List<Document> documents = documentService.getDocumentsByAuthor(author);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取作者文档成功");
            response.put("data", documents);
            response.put("count", documents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取作者文档失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据分类获取文档
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getDocumentsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Document> documents = documentService.getDocumentsByCategory(category, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取分类文档成功");
            response.put("data", documents.getContent());
            response.put("totalElements", documents.getTotalElements());
            response.put("totalPages", documents.getTotalPages());
            response.put("currentPage", documents.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取分类文档失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 综合搜索文档
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchByKeyword(@RequestParam String keyword) {
        try {
            List<Document> documents = documentService.searchByKeyword(keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "搜索完成");
            response.put("data", documents);
            response.put("count", documents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据标签搜索文档
     */
    @GetMapping("/search/tag")
    public ResponseEntity<Map<String, Object>> searchByTag(@RequestParam String tag) {
        try {
            List<Document> documents = documentService.searchByTag(tag);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "标签搜索完成");
            response.put("data", documents);
            response.put("count", documents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "标签搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 更新文档
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDocument(@PathVariable Long id, @RequestBody Document document) {
        try {
            Document updatedDocument = documentService.updateDocument(id, document);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文档更新成功");
            response.put("data", updatedDocument);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文档更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 软删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteDocument(@PathVariable Long id) {
        try {
            boolean deleted = documentService.softDeleteDocument(id);
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "文档删除成功");
            } else {
                response.put("success", false);
                response.put("message", "文档不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文档删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 恢复软删除的文档
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreDocument(@PathVariable Long id) {
        try {
            boolean restored = documentService.restoreDocument(id);
            Map<String, Object> response = new HashMap<>();
            if (restored) {
                response.put("success", true);
                response.put("message", "文档恢复成功");
            } else {
                response.put("success", false);
                response.put("message", "文档不存在或已经是活跃状态");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文档恢复失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 硬删除文档
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Map<String, Object>> permanentDeleteDocument(@PathVariable Long id) {
        try {
            boolean deleted = documentService.deleteDocument(id);
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "文档永久删除成功");
            } else {
                response.put("success", false);
                response.put("message", "文档不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文档永久删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取文档统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "统计信息获取成功");
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalDocuments", documentService.getTotalDocumentCount());
            statistics.put("activeDocuments", documentService.getActiveDocumentCount());
            statistics.put("categoryStats", documentService.getCategoryStatistics());
            statistics.put("authorStats", documentService.getAuthorStatistics());
            
            response.put("data", statistics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
