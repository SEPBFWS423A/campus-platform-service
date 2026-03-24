package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.DegreeType;

public record AdminCourseResponse(
        Long id,
        String name,
        DegreeType degreeType
) {}
