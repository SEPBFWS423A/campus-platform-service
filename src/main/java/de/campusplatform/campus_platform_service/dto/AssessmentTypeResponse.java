package de.campusplatform.campus_platform_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentTypeResponse {
    private String code;
    private Boolean submission;
    private String nameDe;
    private String nameEn;
    private String shortDe;
    private String shortEn;
}