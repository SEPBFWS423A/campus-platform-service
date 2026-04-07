package de.campusplatform.campus_platform_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FaqUpsertRequest(

        @NotNull
        @Min(0)
        Integer sortOrder,

        @NotNull
        Boolean published,

        @NotEmpty
        List<@Valid FaqTranslationRequest> translations
) {
}