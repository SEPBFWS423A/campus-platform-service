package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.ExamType;
import java.util.Set;

public record ModuleRequest(
        String name,
        Integer semester,
        Integer requiredTotalHours,
        java.util.Set<Long> examTypeIds,
        java.util.Set<Long> lecturerIds,
        Long courseOfStudyId,
        Long specializationId,
        Long preferredExamTypeId
) {
}
