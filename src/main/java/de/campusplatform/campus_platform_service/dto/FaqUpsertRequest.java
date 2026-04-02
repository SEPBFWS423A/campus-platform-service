package de.campusplatform.campus_platform_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FaqUpsertRequest(

        @NotBlank
        @Size(max = 255)
        String question,

        @NotBlank
        String answer,

        @NotBlank
        @Size(max = 100)
        String category,

        @NotNull
        @Min(0)
        Integer sortOrder,

        @NotNull
        Boolean published
) {
}