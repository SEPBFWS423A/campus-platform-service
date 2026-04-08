package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;
import de.campusplatform.campus_platform_service.model.Salutation;
import de.campusplatform.campus_platform_service.model.AcademicTitle;

public record AdminUserUpdateRequest(
        Salutation salutation,
        AcademicTitle title,
        String firstName,
        String lastName,
        String email,
        Role role,
        String studentNumber,
        Integer startYear,
        Long specializationId
) {
}
