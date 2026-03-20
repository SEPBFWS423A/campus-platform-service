package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;

public record AdminUserResponse(
        Long id,
        String firstname,
        String lastname,
        String email,
        Role role,
        boolean enabled
) {
}
