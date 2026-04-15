package de.campusplatform.campus_platform_service.dto;

public record CourseDocumentRequest(
        String displayName,
        String fileName,
        String mimeType,
        Long fileSize,
        String contentBase64
) {
}
