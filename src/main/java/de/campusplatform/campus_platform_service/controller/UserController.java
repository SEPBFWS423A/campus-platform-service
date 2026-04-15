package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.service.AuthService;
import de.campusplatform.campus_platform_service.service.FaqService;
import de.campusplatform.campus_platform_service.service.StudentSubmissionService;
import de.campusplatform.campus_platform_service.service.LecturerService;
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
public class UserController {

    private final AuthService authService;
    private final RoomRepository roomRepository;
    private final InstitutionRepository institutionRepository;
    private final FaqService faqService;
    private final StudentSubmissionService studentSubmissionService;
    private final LecturerService lecturerService;


    public UserController(AuthService authService,
                          RoomRepository roomRepository,
                          InstitutionRepository institutionRepository,
                          FaqService faqService,
                          StudentSubmissionService studentSubmissionService,
                          LecturerService lecturerService) {
        this.authService = authService;
        this.roomRepository = roomRepository;
        this.institutionRepository = institutionRepository;
        this.faqService = faqService;
        this.studentSubmissionService = studentSubmissionService;
        this.lecturerService = lecturerService;
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

    // COURSE DOCUMENTS
    @GetMapping("/courses")
    public ResponseEntity<List<LecturerCourseResponse>> getMyCourses(
            @AuthenticationPrincipal de.campusplatform.campus_platform_service.security.CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(lecturerService.getCoursesForStudent(userDetails.appUser().getId()));
    }

    @GetMapping("/course-series/{id}/documents")
    public ResponseEntity<List<CourseDocumentResponse>> getCourseDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(lecturerService.getCourseDocumentsForSeries(id));
    }

    @PostMapping("/course-series/{id}/documents")
    public ResponseEntity<Void> uploadCourseDocument(@PathVariable Long id, @RequestBody CourseDocumentRequest request) {
        lecturerService.uploadCourseDocument(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/course-series/{id}/documents/{documentId}")
    public ResponseEntity<Void> deleteCourseDocument(@PathVariable Long id, @PathVariable Long documentId) {
        lecturerService.deleteCourseDocument(id, documentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/course-series/{id}/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadCourseDocument(@PathVariable Long id, @PathVariable Long documentId) {
        de.campusplatform.campus_platform_service.model.CourseDocument document = lecturerService.getCourseDocumentContent(id, documentId);
        
        byte[] content = java.util.Base64.getDecoder().decode(document.getContentBase64());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(content);
    }
}
