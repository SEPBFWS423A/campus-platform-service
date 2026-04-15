package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AcademicTitle;

public record LecturerListResponse(
    Long id,
    String firstName,
    String lastName,
    AcademicTitle title
) {}
