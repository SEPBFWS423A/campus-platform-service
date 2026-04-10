package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.CourseStatus;

import java.time.LocalDateTime;

import java.util.List;

public record AdminCourseSeriesResponse(
        Long id,
        Long moduleId,
        String moduleName,
        Long assignedLecturerId,
        String assignedLecturerName,
        CourseStatus status,
        Long selectedExamTypeId,
        String selectedExamTypeName,
        LocalDateTime submissionStartDate,
        LocalDateTime submissionDeadline,
        List<StudyGroupDTO> studyGroups
) {
    public record StudyGroupDTO(
            Long id,
            String name
    ) {}
}
