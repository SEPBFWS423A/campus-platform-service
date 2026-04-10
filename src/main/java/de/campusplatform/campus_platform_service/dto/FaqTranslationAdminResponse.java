package de.campusplatform.campus_platform_service.dto;

public record FaqTranslationAdminResponse(
        Long id,
        String languageCode,
        String question,
        String answer,
        String category
) {
}