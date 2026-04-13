package de.campusplatform.campus_platform_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeSummaryResponse {
    private Double currentAverage;
    private Integer achievedEcts;
    private Integer totalEcts;

    private Integer passedModulesCount;
    private Integer failedModulesCount;

    private Integer gradedAssessmentsCount;
    private Integer pendingAssessmentsCount;

    private Integer excusedAbsenceAssessmentsCount;
    private Integer unexcusedAbsenceAssessmentsCount;
    private Integer excludedAssessmentsCount;
}