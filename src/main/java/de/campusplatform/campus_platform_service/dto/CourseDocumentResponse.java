package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record CourseDocumentResponse(
        Long id,
        String displayName,
        String fileName,
        String mimeType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
