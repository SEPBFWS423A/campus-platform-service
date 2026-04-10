package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record FaqAdminResponse(
        Long id,
        Integer sortOrder,
        Boolean published,
        List<FaqTranslationAdminResponse> translations
) {
}