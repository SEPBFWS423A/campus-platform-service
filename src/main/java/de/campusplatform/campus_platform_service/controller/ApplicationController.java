package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.ApplicationResponse;
import de.campusplatform.campus_platform_service.dto.StudyProgramResponse;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final AppUserRepository userRepository;

    public ApplicationController(ApplicationService service, AppUserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping("/programs")
    public ResponseEntity<List<StudyProgramResponse>> getPrograms() {
        return ResponseEntity.ok(service.getActivePrograms());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long studentId = resolveUserId(userDetails);
        return ResponseEntity.ok(service.getMyApplications(studentId));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplicationResponse> apply(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam("programId") Long programId,
        @RequestParam(value = "motivation", required = false) String motivation,
        @RequestParam(value = "priority", defaultValue = "1") Integer priority,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Long studentId = resolveUserId(userDetails);
        return ResponseEntity.ok(service.apply(studentId, programId, motivation, priority, file));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User nicht gefunden"))
            .getId();
    }
}