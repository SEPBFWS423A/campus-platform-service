package de.campusplatform.campus_platform_service.dto;

public record StudyGroupRequest(
        String name,
        Long specializationId,
        Integer startYear,
        Integer startQuartal
) {
}
