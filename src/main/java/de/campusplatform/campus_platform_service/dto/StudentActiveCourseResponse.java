package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.CourseStatus;
import java.time.LocalDateTime;

public record StudentActiveCourseResponse(
        Long id,
        String moduleName,
        String lecturerName,
        CourseStatus status,
        String examTypeName,
        boolean isSubmission,
        LocalDateTime submissionDeadline
) {
}
