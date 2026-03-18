package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class PersonalDetailsRequest {
    private String firstname;
    private String lastname;
    private String email;
}
