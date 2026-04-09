package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record GradeBulkRequest(
        List<StudentGradeItem> grades
) {
    public record StudentGradeItem(
            Long studentId,
            Double grade,
            Double points,
            String feedback
    ) {}
}
