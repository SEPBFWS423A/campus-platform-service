package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AcademicTitle;
import java.util.List;

public record AdminModuleResponse(
        Long id,
        String name,
        Integer semester,
        Integer requiredTotalHours,
        Integer ects, List<ExamTypeDTO> possibleExamTypes,
        Long preferredExamTypeId,
        List<LecturerDTO> lecturers,
        Long courseOfStudyId,
        Long specializationId
) {
    public record ExamTypeDTO(
            Long id,
            String type,
            String nameDe,
            String nameEn,
            String shortDe,
            String shortEn,
            boolean submission
    ) {}

    public record LecturerDTO(
            Long id,
            AcademicTitle title,
            String firstName,
            String lastName
    ) {}
}
