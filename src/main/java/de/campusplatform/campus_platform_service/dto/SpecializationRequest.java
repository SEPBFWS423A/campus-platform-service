package de.campusplatform.campus_platform_service.dto;

public record SpecializationRequest(
        String name,
        Long courseId
) {}
