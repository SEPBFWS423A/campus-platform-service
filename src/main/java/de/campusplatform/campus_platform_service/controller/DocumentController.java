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
    public ResponseEntity<List<GeneralDocumentResponse>> getAllDocuments(@org.springframework.web.bind.annotation.RequestParam(required = false) String category) {
        return ResponseEntity.ok(generalDocumentService.getAllDocuments(category));
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

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ADMIN', 'LECTURER', 'STUDENT')")
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<GeneralDocumentResponse> uploadDocument(@org.springframework.web.bind.annotation.RequestBody de.campusplatform.campus_platform_service.dto.UploadGeneralDocumentRequest request) {
        return ResponseEntity.ok(generalDocumentService.uploadDocument(request));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ADMIN', 'LECTURER', 'STUDENT')")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        generalDocumentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
