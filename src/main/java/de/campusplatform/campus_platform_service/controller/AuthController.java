package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.CompleteRegistrationRequest;
import de.campusplatform.campus_platform_service.dto.LoginRequest;
import de.campusplatform.campus_platform_service.dto.LoginResponse;
import de.campusplatform.campus_platform_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<Void> completeRegistration(@RequestBody CompleteRegistrationRequest request) {
        authService.completeRegistration(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody String email) {
        authService.sendPasswordResetToken(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam("token") String token, @RequestBody String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }
}
