package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class PublicProfileRequest {
    private String bio;
    private String interests;
    private String hobbies;
    private String skills;
    private Boolean visibility;
}
