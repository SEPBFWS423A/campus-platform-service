package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.Salutation;
import de.campusplatform.campus_platform_service.enums.AcademicTitle;
import lombok.Data;

@Data
public class PersonalDetailsRequest {
    private Salutation salutation;
    private AcademicTitle title;
    private String firstName;
    private String lastName;
    private String email;

    // Student fields
    private String studentNumber;
    private Integer startYear;
    private Integer startQuartal;
    private Long specializationId;
}
