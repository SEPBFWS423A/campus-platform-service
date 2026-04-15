package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.GeneralDocumentResponse;
import de.campusplatform.campus_platform_service.dto.UploadGeneralDocumentRequest;
import de.campusplatform.campus_platform_service.service.GeneralDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final GeneralDocumentService generalDocumentService;

    @GetMapping
    public ResponseEntity<List<GeneralDocumentResponse>> getAllDocuments(@org.springframework.web.bind.annotation.RequestParam(required = false) String category) {
        return ResponseEntity.ok(generalDocumentService.getAllDocuments(category));
    }

    @PostMapping
    public ResponseEntity<GeneralDocumentResponse> uploadDocument(@Valid @RequestBody UploadGeneralDocumentRequest request) {
        return ResponseEntity.ok(generalDocumentService.uploadDocument(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        generalDocumentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
