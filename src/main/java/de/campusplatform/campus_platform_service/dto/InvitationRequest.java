package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Role;
import lombok.Data;

@Data
public class InvitationRequest {
    private String email;
    private Role role;
    private String studentNumber;
    private String language;
    private Long specializationId;
    private Integer startYear;
}
