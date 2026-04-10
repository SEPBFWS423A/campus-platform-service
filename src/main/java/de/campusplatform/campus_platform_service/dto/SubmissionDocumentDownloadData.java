package de.campusplatform.campus_platform_service.dto;

public record SubmissionDocumentDownloadData(
        String fileName,
        String mimeType,
        long fileSize,
        byte[] content
) {
}
