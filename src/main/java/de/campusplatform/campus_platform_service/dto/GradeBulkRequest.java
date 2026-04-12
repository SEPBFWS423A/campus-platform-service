package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;

import java.util.List;

public record GradeBulkRequest(
        List<StudentGradeItem> grades
) {
    public record StudentGradeItem(
            Long studentId,
            Double grade,
            Double points,
            String feedback,
            SubmissionStatus status
    ) {
    }
}