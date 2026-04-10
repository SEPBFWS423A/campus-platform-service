package de.campusplatform.campus_platform_service.dto;

public record UploadSubmissionDocumentRequest(
        String fileName,
        String mimeType,
        Long fileSize,
        String contentBase64
) {
}
