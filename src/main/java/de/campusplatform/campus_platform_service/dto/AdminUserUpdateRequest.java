package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.enums.Salutation;
import de.campusplatform.campus_platform_service.enums.AcademicTitle;

public record AdminUserUpdateRequest(
        Salutation salutation,
        AcademicTitle title,
        String firstName,
        String lastName,
        String email,
        Role role,
        String studentNumber,
        Integer startYear,
        Integer startQuartal,
        Long specializationId
) {
}
