package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.service.AuthService;
import de.campusplatform.campus_platform_service.service.FaqService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final AuthService authService;
    private final RoomRepository roomRepository;
    private final InstitutionRepository institutionRepository;
    private final FaqService faqService;



    public UserController(AuthService authService, RoomRepository roomRepository, InstitutionRepository institutionRepository, FaqService faqService) {
        this.authService = authService;
        this.roomRepository = roomRepository;
        this.institutionRepository = institutionRepository;
        this.faqService = faqService;
    }

    @GetMapping("/institution")
    public ResponseEntity<InstitutionInfo> getInstitutionInfo() {
        return institutionRepository.getFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new InstitutionInfo()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
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

    // FAQ
    @GetMapping("/faqs")
    public List<FaqResponse> getVisibleFaqs(@RequestParam(defaultValue = "de") String lang) {
        return faqService.getVisibleFaqs(lang);
    }
}
