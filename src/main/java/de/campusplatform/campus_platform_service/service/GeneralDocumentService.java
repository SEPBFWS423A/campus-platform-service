package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.GeneralDocumentResponse;
import de.campusplatform.campus_platform_service.dto.UploadGeneralDocumentRequest;
import de.campusplatform.campus_platform_service.model.GeneralDocument;
import de.campusplatform.campus_platform_service.repository.GeneralDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneralDocumentService {

    private final GeneralDocumentRepository generalDocumentRepository;

    @Transactional(readOnly = true)
    public List<GeneralDocumentResponse> getAllDocuments() {
        return generalDocumentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public GeneralDocumentResponse uploadDocument(UploadGeneralDocumentRequest request) {
        byte[] decodedContent;
        try {
            decodedContent = Base64.getDecoder().decode(request.contentBase64().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültiger Base64-Inhalt.");
        }

        GeneralDocument document = GeneralDocument.builder()
                .displayName(request.displayName().trim())
                .fileName(request.fileName().trim())
                .mimeType(request.mimeType().trim())
                .fileSize(request.fileSize() != null ? request.fileSize() : (long) decodedContent.length)
                .contentBase64(request.contentBase64().trim())
                .uploadedAt(LocalDateTime.now())
                .build();

        GeneralDocument saved = generalDocumentRepository.save(document);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id) {
        if (!generalDocumentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden.");
        }
        generalDocumentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public GeneralDocument getDocumentEntity(Long id) {
        return generalDocumentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden."));
    }

    private GeneralDocumentResponse mapToResponse(GeneralDocument doc) {
        return new GeneralDocumentResponse(
                doc.getId(),
                doc.getDisplayName(),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getFileSize(),
                doc.getUploadedAt()
        );
    }
}
