package com.example.qa.controller;

import com.example.qa.entity.Document;
import com.example.qa.service.MaterialsImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Materials导入控制器
 * 提供materials.txt文件导入的REST API
 */
@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialsImportController {

    @Autowired
    private MaterialsImportService materialsImportService;

    /**
     * 导入materials.txt内容到数据库
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importMaterials() {
        try {
            List<Document> importedDocuments = materialsImportService.importMaterialsFromFile();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "成功导入materials.txt内容到数据库");
            response.put("importedCount", importedDocuments.size());
            response.put("documents", importedDocuments);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取导入统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取统计信息成功");
            
            // 这里可以添加更多统计信息
            materialsImportService.printImportStatistics();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查导入状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getImportStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "检查导入状态成功");
            response.put("status", "ready");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查导入状态失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取materials.txt文件的原始内容
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getMaterialsContent() {
        try {
            Resource resource = new ClassPathResource("materials.txt");
            if (!resource.exists()) {
                return ResponseEntity.status(404).body("materials.txt文件不存在");
            }
            
            // 使用InputStream读取，支持JAR包中的资源文件
            try (InputStream inputStream = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE + "; charset=utf-8")
                        .body(content);
            }
                    
        } catch (IOException e) {
            return ResponseEntity.status(500).body("读取materials.txt文件失败: " + e.getMessage());
        }
    }
}


