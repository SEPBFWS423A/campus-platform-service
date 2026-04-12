package de.campusplatform.campus_platform_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeSemesterGroupResponse {
    private Integer moduleSemester;
    private List<StudentGradeOverviewItemResponse> items;
}