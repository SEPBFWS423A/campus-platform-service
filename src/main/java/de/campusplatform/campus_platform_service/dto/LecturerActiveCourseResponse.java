package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.CourseStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LecturerActiveCourseResponse(
    Long id,
    String moduleName,
    List<String> studyGroups,
    CourseStatus status,
    String examTypeName,
    boolean isSubmission,
    LocalDateTime submissionDeadline,
    Long attendeeCount
) {}
