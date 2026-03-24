package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class CompleteRegistrationRequest {
    private String token;
    private String firstName;
    private String lastName;
    private String password;

    // Student profile details (only used if role is student)
    private String studentNumber;
    private Integer startYear;
    private Long specializationId;
}
