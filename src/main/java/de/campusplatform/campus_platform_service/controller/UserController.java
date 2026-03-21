package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.ChangePasswordRequest;
import de.campusplatform.campus_platform_service.dto.PersonalDetailsRequest;
import de.campusplatform.campus_platform_service.dto.UserPreferencesRequest;
import de.campusplatform.campus_platform_service.dto.UserProfileResponse;
import de.campusplatform.campus_platform_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(authService.getUserProfile(userDetails.getUsername()));
    }

    @PutMapping("/profile/details")
    public ResponseEntity<Void> updatePersonalDetails(
            @RequestBody PersonalDetailsRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        authService.updatePersonalDetails(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/profile/preferences")
    public ResponseEntity<Void> updateUserPreferences(
            @RequestBody UserPreferencesRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        authService.updateUserPreferences(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}
