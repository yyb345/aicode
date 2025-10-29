package com.example.qa.controller;

import com.example.qa.service.ConfluenceImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Confluence导入控制器
 * 提供Confluence文档导入到SQLite数据库的REST API接口
 */
@RestController
@RequestMapping("/api/confluence")
@CrossOrigin(origins = "*")
public class ConfluenceImportController {
    
    @Autowired
    private ConfluenceImportService confluenceImportService;
    
    /**
     * 导入指定时间范围内的Confluence文档
     */
    @PostMapping("/import/days/{days}")
    public ResponseEntity<Map<String, Object>> importDocumentsByDays(@PathVariable int days) {
        try {
            ConfluenceImportService.ImportResult result = confluenceImportService.importDocumentsFromConfluence(days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("data", Map.of(
                "successCount", result.getSuccessCount(),
                "skipCount", result.getSkipCount(),
                "totalProcessed", result.getTotalProcessed(),
                "errors", result.getErrors()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 导入所有Confluence文档
     */
    @PostMapping("/import/all")
    public ResponseEntity<Map<String, Object>> importAllDocuments() {
        try {
            ConfluenceImportService.ImportResult result = confluenceImportService.importAllDocumentsFromConfluence();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("data", Map.of(
                "successCount", result.getSuccessCount(),
                "skipCount", result.getSkipCount(),
                "totalProcessed", result.getTotalProcessed(),
                "errors", result.getErrors()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 导入指定页面ID的文档
     */
    @PostMapping("/import/page/{pageId}")
    public ResponseEntity<Map<String, Object>> importDocumentByPageId(@PathVariable String pageId) {
        try {
            ConfluenceImportService.ImportResult result = confluenceImportService.importDocumentByPageId(pageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("data", Map.of(
                "successCount", result.getSuccessCount(),
                "skipCount", result.getSkipCount(),
                "totalProcessed", result.getTotalProcessed(),
                "errors", result.getErrors()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取导入状态和统计信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getImportStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "导入服务状态正常");
            response.put("data", Map.of(
                "serviceAvailable", true,
                "timestamp", System.currentTimeMillis()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取状态失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
