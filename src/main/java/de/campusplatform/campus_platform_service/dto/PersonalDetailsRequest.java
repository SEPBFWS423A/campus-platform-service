package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class PersonalDetailsRequest {
    private String firstName;
    private String lastName;
    private String email;

    // Student fields
    private String studentNumber;
    private Integer startYear;
    private Long specializationId;
}
