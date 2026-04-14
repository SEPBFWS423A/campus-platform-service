package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.GeneralDocumentResponse;
import de.campusplatform.campus_platform_service.model.GeneralDocument;
import de.campusplatform.campus_platform_service.service.GeneralDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final GeneralDocumentService generalDocumentService;

    @GetMapping
    public ResponseEntity<List<GeneralDocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(generalDocumentService.getAllDocuments());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        GeneralDocument document = generalDocumentService.getDocumentEntity(id);
        
        byte[] content = Base64.getDecoder().decode(document.getContentBase64());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(content);
    }
}
