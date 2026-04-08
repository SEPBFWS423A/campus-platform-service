package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Salutation;
import de.campusplatform.campus_platform_service.model.AcademicTitle;
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
    private Long specializationId;
}
