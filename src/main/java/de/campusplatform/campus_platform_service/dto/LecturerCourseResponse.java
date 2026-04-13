package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.List;

public record LecturerCourseResponse(
        Long id,
        String moduleName,
        List<String> studyGroupNames,
        String examTypeName,
        boolean submission,
        ExamStatus examStatus,
        String examFileName,
        String solutionFileName,
        String lecturerNotes,
        LocalDateTime submissionDeadline,
        List<EventResponse> events,
        Long submissionCount
) {
    public record EventResponse(
            Long id,
            String type,
            LocalDateTime start,
            LocalDateTime end,
            String roomName,
            Integer roomExamSeats
    ) {}
}
