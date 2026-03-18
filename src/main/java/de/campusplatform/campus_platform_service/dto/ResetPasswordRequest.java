package de.campusplatform.campus_platform_service.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordRequest {
    private String newPassword;
}
