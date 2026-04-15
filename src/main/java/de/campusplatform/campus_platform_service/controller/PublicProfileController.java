package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.PublicProfileRequest;
import de.campusplatform.campus_platform_service.dto.PublicProfileResponse;
import de.campusplatform.campus_platform_service.service.PublicProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/social/profile")
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    public PublicProfileController(PublicProfileService publicProfileService) {
        this.publicProfileService = publicProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<PublicProfileResponse> getMyProfile(Principal principal) {
        return publicProfileService.getProfileByEmail(principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/me")
    public ResponseEntity<PublicProfileResponse> joinSocialHub(@RequestBody PublicProfileRequest request, Principal principal) {
        return ResponseEntity.status(201).body(publicProfileService.createOrUpdateProfile(principal.getName(), request));
    }

    @PutMapping("/me")
    public ResponseEntity<PublicProfileResponse> updateMyProfile(@RequestBody PublicProfileRequest request, Principal principal) {
        return ResponseEntity.ok(publicProfileService.createOrUpdateProfile(principal.getName(), request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicProfileResponse>> searchFellowStudents(@RequestParam String q, Principal principal) {
        return ResponseEntity.ok(publicProfileService.searchProfiles(q, principal.getName()));
    }
}
