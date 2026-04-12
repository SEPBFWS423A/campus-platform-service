package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;

public record SingleGradeRequest(
        Long studentId,
        Double grade,
        Double points,
        String feedback,
        SubmissionStatus status
) {
}