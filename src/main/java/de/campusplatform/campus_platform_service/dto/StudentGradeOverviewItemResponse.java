package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.StudentGradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeOverviewItemResponse {
    private Long courseSeriesId;
    private Long moduleId;

    private String moduleName;
    private Integer moduleSemester;

    private List<String> studyGroupNames;

    private AssessmentTypeResponse assessmentType;

    private LocalDate examDate;
    private AcademicTermResponse academicTerm;

    private Integer ects;

    private StudentGradeStatus status;
    private Integer attemptNumber;

    private Double grade;
    private String reviewerComment;

    private LocalDateTime lastUpdatedAt;
}