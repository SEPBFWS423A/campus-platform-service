package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.CourseStatus;

import java.time.LocalDateTime;

import java.util.List;

public record CourseSeriesRequest(
        Long moduleId,
        Long assignedLecturerId,
        CourseStatus status,
        Long selectedExamTypeId,
        LocalDateTime submissionStartDate,
        LocalDateTime submissionDeadline,
        List<Long> studyGroupIds
) {}
