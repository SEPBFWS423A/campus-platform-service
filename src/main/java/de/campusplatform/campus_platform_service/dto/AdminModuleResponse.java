package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record AdminModuleResponse(
        Long id,
        String name,
        Integer semester,
        Integer requiredTotalHours,
        List<ExamTypeDTO> possibleExamTypes,
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
            String shortEn
    ) {}

    public record LecturerDTO(
            Long id,
            String title,
            String firstName,
            String lastName
    ) {}
}
