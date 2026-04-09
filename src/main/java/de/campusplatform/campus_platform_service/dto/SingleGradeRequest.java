package de.campusplatform.campus_platform_service.dto;

public record SingleGradeRequest(
    Long studentId,
    Double grade,
    Double points,
    String feedback
) {}
