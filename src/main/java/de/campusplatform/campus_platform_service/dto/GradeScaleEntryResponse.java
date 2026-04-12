package de.campusplatform.campus_platform_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScaleEntryResponse {
    private Double grade;
    private Double minimumPoints;
    private String label;
}