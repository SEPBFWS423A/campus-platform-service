package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;
import de.campusplatform.campus_platform_service.model.Salutation;
import de.campusplatform.campus_platform_service.model.AcademicTitle;

public record AdminUserResponse(
        Long id,
        Salutation salutation,
        AcademicTitle title,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        String studentNumber,
        Integer startYear,
        Integer startQuartal,
        Long specializationId,
        String specializationName,
        Long courseOfStudyId,
        String courseOfStudyName
) {
}
