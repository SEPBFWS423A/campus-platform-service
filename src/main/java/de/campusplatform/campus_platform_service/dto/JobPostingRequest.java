package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobPostingRequest(
        @NotBlank String title,
        @NotBlank String department,
        @NotNull JobType type,
        @NotBlank String description,
        String requirements,
        @NotNull LocalDate deadline,
        Boolean autoPublish
) {}
