package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.JobStatus;
import de.campusplatform.campus_platform_service.enums.JobType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobPostingResponse(
        Long id,
        String title,
        String department,
        JobType type,
        JobStatus status,
        String description,
        String requirements,
        LocalDate deadline,
        LocalDateTime createdAt,
        Integer applicationCount,
        Boolean autoPublish,
        String createdBy
) {}
