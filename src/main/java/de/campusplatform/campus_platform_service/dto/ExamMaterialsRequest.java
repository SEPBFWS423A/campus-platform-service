package de.campusplatform.campus_platform_service.dto;

public record ExamMaterialsRequest(
        String examFileName,
        String examContent,
        String solutionFileName,
        String solutionContent,
        String lecturerNotes
) {}
