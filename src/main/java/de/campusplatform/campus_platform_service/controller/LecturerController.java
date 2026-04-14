package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.security.CustomUserDetails;
import de.campusplatform.campus_platform_service.service.LecturerService;
import de.campusplatform.campus_platform_service.service.LecturerAbsenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lecturer")
@PreAuthorize("hasAnyAuthority('LECTURER', 'ADMIN')")
public class LecturerController {

    private final LecturerService lecturerService;
    private final LecturerAbsenceService lecturerAbsenceService;

    public LecturerController(LecturerService lecturerService, LecturerAbsenceService lecturerAbsenceService) {
        this.lecturerService = lecturerService;
        this.lecturerAbsenceService = lecturerAbsenceService;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<LecturerCourseResponse>> getCourses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lecturerService.getCoursesForLecturer(userDetails.appUser().getId()));
    }
    
    @GetMapping("/timetable/events")
    public ResponseEntity<List<LecturerEventResponse>> getTimetableEvents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lecturerService.getTimetableEvents(userDetails.appUser().getId()));
    }

    @GetMapping("/timetable/active-series")
    public ResponseEntity<List<LecturerActiveCourseResponse>> getActiveCourseSeries(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lecturerService.getActiveCourseSeries(userDetails.appUser().getId()));
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

    @GetMapping("/course-series/{id}/student-submissions/{studentId}/download")
    public ResponseEntity<SubmissionDocumentDownloadData> downloadStudentSubmission(@PathVariable Long id, @PathVariable Long studentId) {
        return ResponseEntity.ok(lecturerService.getStudentSubmissionDocument(id, studentId));
    }

    @GetMapping("/absences")
    public ResponseEntity<List<LecturerAbsenceResponse>> getMyAbsences(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lecturerAbsenceService.getMyAbsences(userDetails.appUser().getId()));
    }

    @GetMapping("/absences/conflicts")
    public ResponseEntity<List<ConflictingEventDto>> checkConflicts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String start,
            @RequestParam String end) {
        Long lecturerId = userDetails.appUser().getId();
        List<ConflictingEventDto> conflicts = lecturerAbsenceService.getConflictingEvents(
                lecturerId,
                LocalDateTime.parse(start),
                LocalDateTime.parse(end));
        return ResponseEntity.ok(conflicts);
    }

    @PostMapping("/absences")
    public ResponseEntity<LecturerAbsenceResponse> createAbsence(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody LecturerAbsenceRequest request,
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(lecturerAbsenceService.createAbsence(userDetails.appUser().getId(), request, force));
    }

    @DeleteMapping("/absences/{id}")
    public ResponseEntity<Void> deleteAbsence(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long id) {
        lecturerAbsenceService.deleteAbsence(id, userDetails.appUser().getId(), false);
        return ResponseEntity.ok().build();
    }
}
