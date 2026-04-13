package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AcademicTermSeason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTermResponse {
    private AcademicTermSeason season;
    private Integer startYear;
    private Integer endYear;
}