package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.enums.Salutation;
import de.campusplatform.campus_platform_service.enums.AcademicTitle;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserProfileResponse {
    private Long id;
    private Salutation salutation;
    private AcademicTitle title;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String theme;
    private String brightness;
    private String language;

    // Student extension details (nullable)
    private String studentNumber;
    private Integer startYear;
    private Integer startQuartal;
    private Long specializationId;
    private String specializationName;
    private String courseOfStudyName;

    public UserProfileResponse(Long id, Salutation salutation, AcademicTitle title, String email, String firstName, String lastName, Role role, String theme, String brightness, String language) {
        this.id = id;
        this.salutation = salutation;
        this.title = title;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.theme = theme;
        this.brightness = brightness;
        this.language = language;
    }

}
