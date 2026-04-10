package de.campusplatform.campus_platform_service.dto;

public record FaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        Integer sortOrder,
        Boolean published
) {
}