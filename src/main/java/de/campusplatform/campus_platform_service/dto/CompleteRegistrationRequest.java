package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Salutation;
import de.campusplatform.campus_platform_service.model.AcademicTitle;
import lombok.Data;

@Data
public class CompleteRegistrationRequest {
    private String token;
    private Salutation salutation;
    private AcademicTitle title;
    private String firstName;
    private String lastName;
    private String password;

    // Student profile details (only used if role is student)
    private String studentNumber;
    private Integer startYear;
    private Integer startQuartal;
    private Long specializationId;
}
