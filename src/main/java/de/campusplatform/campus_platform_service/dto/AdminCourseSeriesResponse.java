package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.CourseStatus;
import de.campusplatform.campus_platform_service.enums.ExamStatus;

import java.time.LocalDateTime;

import java.util.List;

public record AdminCourseSeriesResponse(
        Long id,
        Long moduleId,
        String moduleName,
        Long assignedLecturerId,
        String assignedLecturerName,
        CourseStatus status,
        ExamStatus examStatus,
        Long selectedExamTypeId,
        String selectedExamTypeName,
        LocalDateTime submissionStartDate,
        LocalDateTime submissionDeadline,
        boolean submission,
        String examFileName,
        String solutionFileName,
        String lecturerNotes,
        Long submissionCount,
        List<StudyGroupDTO> studyGroups,
        List<EventResponse> events
) {
    public record StudyGroupDTO(
            Long id,
            String name
    ) {}

    public record EventResponse(
            Long id,
            String type,
            LocalDateTime start,
            LocalDateTime end,
            String roomName,
            Integer roomExamSeats
    ) {}
}
