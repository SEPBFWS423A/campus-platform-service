package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.model.GradeScaleEntry;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.security.CustomUserDetails;
import de.campusplatform.campus_platform_service.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final RoomRepository roomRepository;
    private final InstitutionRepository institutionRepository;
    private final StudentSubmissionService studentSubmissionService;
    private final StudentGradeService studentGradeService;
    private final StudentDashboardService studentDashboardService;
    private final GradeScaleService gradeScaleService;

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

    @GetMapping("/dashboard")
    public ResponseEntity<StudentDashboardResponse> getStudentDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            studentDashboardService.getDashboard(userDetails.getUsername())
        );
    }

    // STUDENT SUBMISSIONS

    @GetMapping("/submissions")
    public ResponseEntity<List<StudentSubmissionListItemResponse>> getMySubmissions(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                studentSubmissionService.getMySubmissions(userDetails.getUsername())
        );
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<StudentSubmissionDetailResponse> getMySubmissionDetail(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                studentSubmissionService.getMySubmissionDetail(submissionId, userDetails.getUsername())
        );
    }

    @PostMapping("/submissions/{submissionId}/documents")
    public ResponseEntity<SubmissionDocumentResponse> uploadDocument(
            @PathVariable Long submissionId,
            @RequestBody UploadSubmissionDocumentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        SubmissionDocumentResponse response = studentSubmissionService.uploadDocument(
                submissionId,
                request,
                userDetails.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/submissions/{submissionId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long submissionId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        studentSubmissionService.deleteDocument(submissionId, documentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/submissions/{submissionId}/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long submissionId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        SubmissionDocumentDownloadData download = studentSubmissionService.downloadDocument(
                submissionId,
                documentId,
                userDetails.getUsername()
        );

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.mimeType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String contentDisposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(mediaType)
                .contentLength(download.fileSize())
                .body(download.content());
    }

    @PostMapping("/submissions/{submissionId}/submit")
    public ResponseEntity<Void> submitSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        studentSubmissionService.submitSubmission(submissionId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // Notenübersicht

    @GetMapping("/grades/overview")
    public ResponseEntity<StudentGradeOverviewResponse> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                studentGradeService.getOverviewForStudent(userDetails.appUser().getId())
        );
    }

    @GetMapping("/grade-scale")
    public ResponseEntity<List<GradeScaleEntry>> getGradeScale() {
        return ResponseEntity.ok(gradeScaleService.getAllEntries());
    }
}
