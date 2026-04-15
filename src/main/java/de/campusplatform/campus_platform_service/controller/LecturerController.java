package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.security.CustomUserDetails;
import de.campusplatform.campus_platform_service.service.LecturerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/lecturer")
@PreAuthorize("hasAuthority('LECTURER')")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<LecturerCourseResponse>> getCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lecturerService.getCoursesForLecturer(userDetails.appUser().getId()));
    }

    @PostMapping("/course-series/{id}/exam-materials")
    public ResponseEntity<Void> uploadExamMaterials(@PathVariable Long id, @RequestBody ExamMaterialsRequest request) {
        lecturerService.updateExamMaterials(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/course-series/{id}/submissions")
    public ResponseEntity<List<StudentSubmissionResponse>> getSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(lecturerService.getSubmissionsForSeries(id));
    }

    @PostMapping("/course-series/{id}/grades")
    public ResponseEntity<Void> bulkApplyGrades(@PathVariable Long id, @RequestBody GradeBulkRequest request) {
        lecturerService.bulkApplyGrades(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/course-series/{id}/publish")
    public ResponseEntity<Void> publishGrades(@PathVariable Long id) {
        lecturerService.publishGrades(id);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/course-series/{id}/download-document")
    public ResponseEntity<ExamDocumentResponse> downloadDocument(@PathVariable Long id, @RequestParam de.campusplatform.campus_platform_service.enums.ExamDocumentType type) {
        return ResponseEntity.ok(lecturerService.getExamDocument(id, type));
    }

    @PutMapping("/course-series/{id}/single-grade")
    public ResponseEntity<StudentSubmissionResponse> applySingleGrade(@PathVariable Long id, @RequestBody SingleGradeRequest item) {
        return ResponseEntity.ok(lecturerService.applySingleGrade(id, item));
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
        
        byte[] content = Base64.getDecoder().decode(document.getContentBase64());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(content);
    }
}
