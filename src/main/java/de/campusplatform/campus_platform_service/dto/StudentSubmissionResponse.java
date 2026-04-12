package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record StudentSubmissionResponse(
        Long studentId,
        String studentName,
        String studentNumber,
        SubmissionStatus status,
        String documentUrl,
        LocalDateTime submissionDate,
        Double grade,
        Double points,
        String feedback,
        String studyGroupName
) {}
