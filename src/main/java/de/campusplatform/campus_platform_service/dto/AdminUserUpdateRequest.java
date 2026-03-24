package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;

public record AdminUserUpdateRequest(
        String firstName,
        String lastName,
        String email,
        Role role,
        String studentNumber,
        Integer startYear,
        Long specializationId
) {
}
