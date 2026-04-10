package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record SubmissionDocumentResponse(
        Long id,
        String fileName,
        String mimeType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
