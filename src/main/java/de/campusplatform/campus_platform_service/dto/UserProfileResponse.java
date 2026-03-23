package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
    private String theme;
    private String brightness;

    // Student extension details (nullable)
    private String studentNumber;
    private Integer startYear;
    private Long focusId;
    private String focusName;
    private String courseOfStudyName;

    public UserProfileResponse(Long id, String email, String firstname, String lastname, Role role, String theme, String brightness) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
        this.theme = theme;
        this.brightness = brightness;
    }

}
