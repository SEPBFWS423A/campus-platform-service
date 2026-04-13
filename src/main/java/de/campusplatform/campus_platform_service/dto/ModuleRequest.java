package de.campusplatform.campus_platform_service.dto;

import java.util.Set;

public record ModuleRequest(
        String name,
        Integer semester,
        Integer requiredTotalHours,
        Integer ects,
        Set<Long> examTypeIds,
        Set<Long> lecturerIds,
        Long courseOfStudyId,
        Long specializationId,
        Long preferredExamTypeId
) {
}
