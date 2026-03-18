package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class CompleteRegistrationRequest {
    private String token;
    private String firstname;
    private String lastname;
    private String password;
}
