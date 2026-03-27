package de.campusplatform.campus_platform_service.dto;

import lombok.Data;

@Data
public class UserPreferencesRequest {
    private String theme;
    private String brightness;
    private String language;
}
