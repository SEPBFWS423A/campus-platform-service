package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.InvitationRequest;
import de.campusplatform.campus_platform_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteUser(@RequestBody InvitationRequest request) {
        authService.inviteUser(request);
        return ResponseEntity.ok().build();
    }
}
