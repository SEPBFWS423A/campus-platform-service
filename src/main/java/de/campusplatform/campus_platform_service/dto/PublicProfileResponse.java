package de.campusplatform.campus_platform_service.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String studyGroupName;
    private String bio;
    private String interests;
    private String hobbies;
    private String skills;
    private boolean visibility;
}
