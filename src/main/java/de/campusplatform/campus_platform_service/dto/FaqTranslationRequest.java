package de.campusplatform.campus_platform_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqTranslationRequest(

        @NotBlank
        @Size(max = 10)
        String languageCode,

        @NotBlank
        @Size(max = 255)
        String question,

        @NotBlank
        String answer,

        @NotBlank
        @Size(max = 100)
        String category
) {
}