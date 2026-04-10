package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record StudentSubmissionListItemResponse(
        Long submissionId,
        Long courseSeriesId,
        String examTypeName,
        SubmissionStatus status,
        LocalDateTime submissionStartDate,
        LocalDateTime submissionDeadline,
        boolean hasDocuments,
        boolean missingDocuments,
        boolean editable,
        boolean overdue,
        Double grade,
        Double points
) {
}
