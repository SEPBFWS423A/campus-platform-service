package de.campusplatform.campus_platform_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadGeneralDocumentRequest(
    @NotBlank String displayName,
    @NotBlank String fileName,
    @NotBlank String mimeType,
    @NotBlank String contentBase64,
    Long fileSize,
    String category
) {}
