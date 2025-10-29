package com.example.qa.service;

import com.example.qa.entity.Document;
import com.example.qa.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文档服务测试类
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private DocumentRepository documentRepository;
    
    @InjectMocks
    private DocumentService documentService;
    
    private Document testDocument;
    
    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setTitle("测试文档");
        testDocument.setContent("这是一个测试文档的内容");
        testDocument.setAuthor("测试作者");
        testDocument.setCategory("技术文档");
        testDocument.setTags("测试,文档,技术");
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setIsActive(true);
    }
    
    @Test
    void testCreateDocument() {
        // Given
        Document newDocument = new Document("新文档", "新文档内容");
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        
        // When
        Document result = documentService.createDocument(newDocument);
        
        // Then
        assertNotNull(result);
        assertEquals(testDocument.getId(), result.getId());
        assertEquals(testDocument.getTitle(), result.getTitle());
        assertTrue(result.getIsActive());
        assertNotNull(result.getCreatedAt());
        
        verify(documentRepository).save(any(Document.class));
    }
    
    @Test
    void testGetDocumentById() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        
        // When
        Optional<Document> result = documentService.getDocumentById(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testDocument.getId(), result.get().getId());
        assertEquals(testDocument.getTitle(), result.get().getTitle());
        
        verify(documentRepository).findById(1L);
    }
    
    @Test
    void testGetDocumentByIdNotFound() {
        // Given
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When
        Optional<Document> result = documentService.getDocumentById(999L);
        
        // Then
        assertFalse(result.isPresent());
        verify(documentRepository).findById(999L);
    }
    
    @Test
    void testGetAllActiveDocuments() {
        // Given
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByIsActiveTrue()).thenReturn(documents);
        
        // When
        List<Document> result = documentService.getAllActiveDocuments();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        
        verify(documentRepository).findByIsActiveTrue();
    }
    
    @Test
    void testSearchByTitle() {
        // Given
        String title = "测试";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByTitleContainingIgnoreCase(title)).thenReturn(documents);
        
        // When
        List<Document> result = documentService.searchByTitle(title);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        
        verify(documentRepository).findByTitleContainingIgnoreCase(title);
    }
    
    @Test
    void testGetDocumentsByAuthor() {
        // Given
        String author = "测试作者";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByAuthor(author)).thenReturn(documents);
        
        // When
        List<Document> result = documentService.getDocumentsByAuthor(author);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        
        verify(documentRepository).findByAuthor(author);
    }
    
    @Test
    void testGetDocumentsByCategory() {
        // Given
        String category = "技术文档";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByCategory(category)).thenReturn(documents);
        
        // When
        List<Document> result = documentService.getDocumentsByCategory(category);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        
        verify(documentRepository).findByCategory(category);
    }
    
    @Test
    void testSearchByKeyword() {
        // Given
        String keyword = "测试";
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findByKeyword(keyword)).thenReturn(documents);
        
        // When
        List<Document> result = documentService.searchByKeyword(keyword);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDocument.getId(), result.get(0).getId());
        
        verify(documentRepository).findByKeyword(keyword);
    }
    
    @Test
    void testUpdateDocument() {
        // Given
        Document updatedDocument = new Document();
        updatedDocument.setTitle("更新后的标题");
        updatedDocument.setContent("更新后的内容");
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        
        // When
        Document result = documentService.updateDocument(1L, updatedDocument);
        
        // Then
        assertNotNull(result);
        verify(documentRepository).findById(1L);
        verify(documentRepository).save(any(Document.class));
    }
    
    @Test
    void testUpdateDocumentNotFound() {
        // Given
        Document updatedDocument = new Document();
        updatedDocument.setTitle("更新后的标题");
        
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            documentService.updateDocument(999L, updatedDocument);
        });
        
        verify(documentRepository).findById(999L);
        verify(documentRepository, never()).save(any(Document.class));
    }
    
    @Test
    void testSoftDeleteDocument() {
        // Given
        when(documentRepository.softDeleteById(1L, any(LocalDateTime.class))).thenReturn(1);
        
        // When
        boolean result = documentService.softDeleteDocument(1L);
        
        // Then
        assertTrue(result);
        verify(documentRepository).softDeleteById(1L, any(LocalDateTime.class));
    }
    
    @Test
    void testSoftDeleteDocumentNotFound() {
        // Given
        when(documentRepository.softDeleteById(999L, any(LocalDateTime.class))).thenReturn(0);
        
        // When
        boolean result = documentService.softDeleteDocument(999L);
        
        // Then
        assertFalse(result);
        verify(documentRepository).softDeleteById(999L, any(LocalDateTime.class));
    }
    
    @Test
    void testRestoreDocument() {
        // Given
        when(documentRepository.restoreById(1L, any(LocalDateTime.class))).thenReturn(1);
        
        // When
        boolean result = documentService.restoreDocument(1L);
        
        // Then
        assertTrue(result);
        verify(documentRepository).restoreById(1L, any(LocalDateTime.class));
    }
    
    @Test
    void testDeleteDocument() {
        // Given
        when(documentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(documentRepository).deleteById(1L);
        
        // When
        boolean result = documentService.deleteDocument(1L);
        
        // Then
        assertTrue(result);
        verify(documentRepository).existsById(1L);
        verify(documentRepository).deleteById(1L);
    }
    
    @Test
    void testDeleteDocumentNotFound() {
        // Given
        when(documentRepository.existsById(999L)).thenReturn(false);
        
        // When
        boolean result = documentService.deleteDocument(999L);
        
        // Then
        assertFalse(result);
        verify(documentRepository).existsById(999L);
        verify(documentRepository, never()).deleteById(anyLong());
    }
    
    @Test
    void testDocumentExists() {
        // Given
        when(documentRepository.existsById(1L)).thenReturn(true);
        
        // When
        boolean result = documentService.documentExists(1L);
        
        // Then
        assertTrue(result);
        verify(documentRepository).existsById(1L);
    }
    
    @Test
    void testDocumentExistsByTitleAndAuthor() {
        // Given
        String title = "测试文档";
        String author = "测试作者";
        when(documentRepository.findByTitleAndAuthor(title, author)).thenReturn(Optional.of(testDocument));
        
        // When
        boolean result = documentService.documentExistsByTitleAndAuthor(title, author);
        
        // Then
        assertTrue(result);
        verify(documentRepository).findByTitleAndAuthor(title, author);
    }
    
    @Test
    void testGetTotalDocumentCount() {
        // Given
        when(documentRepository.count()).thenReturn(10L);
        
        // When
        long result = documentService.getTotalDocumentCount();
        
        // Then
        assertEquals(10L, result);
        verify(documentRepository).count();
    }
}
