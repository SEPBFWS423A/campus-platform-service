package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;

public record AdminUserResponse(
        Long id,
        String salutation,
        String title,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        String studentNumber,
        Integer startYear,
        Long specializationId,
        String specializationName,
        String courseOfStudyName
) {
}
