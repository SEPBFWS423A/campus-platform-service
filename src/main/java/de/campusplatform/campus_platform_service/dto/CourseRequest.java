package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.DegreeType;

public record CourseRequest(
        String name,
        DegreeType degreeType
) {}
