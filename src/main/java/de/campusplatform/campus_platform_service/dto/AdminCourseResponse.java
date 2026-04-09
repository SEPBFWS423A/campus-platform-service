package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.DegreeType;

public record AdminCourseResponse(
        Long id,
        String name,
        DegreeType degreeType
) {}
