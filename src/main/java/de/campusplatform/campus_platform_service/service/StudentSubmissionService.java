package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.StudentSubmissionDetailResponse;
import de.campusplatform.campus_platform_service.dto.StudentSubmissionListItemResponse;
import de.campusplatform.campus_platform_service.dto.SubmissionDocumentDownloadData;
import de.campusplatform.campus_platform_service.dto.SubmissionDocumentResponse;
import de.campusplatform.campus_platform_service.dto.UploadSubmissionDocumentRequest;
import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.model.SubmissionDocument;
import de.campusplatform.campus_platform_service.model.ExamType;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.StudentCourseSubmissionRepository;
import de.campusplatform.campus_platform_service.repository.SubmissionDocumentRepository;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentSubmissionService {

    private static final long MAX_FILE_SIZE_BYTES = 1_572_864L; // 1.5 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf",
            ".xlsx",
            ".pptx"
    );

    private final StudentCourseSubmissionRepository submissionRepository;
    private final SubmissionDocumentRepository submissionDocumentRepository;
    private final AppUserRepository appUserRepository;
    private final de.campusplatform.campus_platform_service.repository.EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<StudentSubmissionListItemResponse> getMySubmissions(String username) {
        AppUser student = getCurrentUser(username);

        return submissionRepository.findByStudentId(student.getId())
                .stream()
                .filter(sub -> isStudentEligibleForCourse(student, sub.getCourseSeries()))
                .sorted(Comparator.comparing(
                        this::resolveEffectiveDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toListItemResponse)
                .toList();
    }

    private boolean isStudentEligibleForCourse(AppUser student, CourseSeries series) {
        if (series == null || series.getStudyGroups() == null) {
            return false;
        }

        // The student should only see submissions when the CourseSeries is ACTIVE, GRADING, or COMPLETED
        if (series.getStatus() == de.campusplatform.campus_platform_service.enums.CourseStatus.PLANNED) {
            return false;
        }
        
        // A student is eligible if they are in any of the study groups assigned to the course series
        return series.getStudyGroups().stream()
                .anyMatch(group -> group.getMemberships().stream()
                        .anyMatch(m -> m.getStudent().getAppUser().getId().equals(student.getId())));
    }

    @Transactional
    public void initializeSubmissionsForCourseSeries(Long seriesId) {
        CourseSeries series = submissionRepository.findCourseSeriesWithGroupsById(seriesId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kurs nicht gefunden."));

        List<AppUser> students = appUserRepository.findStudentsByCourseSeriesId(seriesId);

        for (AppUser student : students) {
            if (submissionRepository.findByCourseSeriesIdAndStudentId(seriesId, student.getId()).isEmpty()) {
                StudentCourseSubmission submission = StudentCourseSubmission.builder()
                        .courseSeries(series)
                        .student(student)
                        .status(SubmissionStatus.PENDING)
                        .build();
                submissionRepository.save(submission);
            }
        }
    }

    @Transactional
    public void cleanupSubmissionsIfNoKlausurExists(Long seriesId) {
        CourseSeries series = submissionRepository.findCourseSeriesWithGroupsById(seriesId).orElse(null);
        if (series == null) return;

        ExamType examType = series.getSelectedExamType();
        if (examType == null && series.getModule() != null) {
            examType = series.getModule().getPreferredExamType();
        }

        // If it's a SUBMISSION type, we never cleanup based on Klausur events
        if (examType != null && examType.isSubmission()) {
            return;
        }

        // For WRITTEN (or undefined), we still require a Klausur event to keep submissions
        boolean hasKlausur = eventRepository.existsByCourseSeriesIdAndEventType(seriesId, EventType.KLAUSUR);
        
        if (!hasKlausur) {
            // Remove all submissions for this series that are still PENDING and have no documents
            List<StudentCourseSubmission> subs = submissionRepository.findByCourseSeriesId(seriesId);
            for (StudentCourseSubmission sub : subs) {
                if (sub.getStatus() == SubmissionStatus.PENDING && (sub.getDocuments() == null || sub.getDocuments().isEmpty())) {
                    submissionRepository.delete(sub);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public StudentSubmissionDetailResponse getMySubmissionDetail(Long submissionId, String username) {
        AppUser student = getCurrentUser(username);
        StudentCourseSubmission submission = getOwnedSubmission(submissionId, student.getId());
        return toDetailResponse(submission);
    }

    @Transactional
    public SubmissionDocumentResponse uploadDocument(Long submissionId,
                                                     UploadSubmissionDocumentRequest request,
                                                     String username) {
        AppUser student = getCurrentUser(username);
        StudentCourseSubmission submission = getOwnedSubmission(submissionId, student.getId());

        ensureEditable(submission);
        validateUploadRequest(request);

        byte[] decodedContent;
        try {
            decodedContent = Base64.getDecoder().decode(request.contentBase64().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültiger Base64-Inhalt.");
        }

        if (decodedContent.length > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datei überschreitet 1,5 MB.");
        }

        if (request.fileSize() != null && !request.fileSize().equals((long) decodedContent.length)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dateigröße stimmt nicht mit dem Inhalt überein.");
        }

        SubmissionDocument document = SubmissionDocument.builder()
                .submission(submission)
                .fileName(request.fileName().trim())
                .mimeType(request.mimeType().trim())
                .fileSize((long) decodedContent.length)
                .contentBase64(Base64.getEncoder().encodeToString(decodedContent))
                .uploadedAt(LocalDateTime.now())
                .build();

        submission.getDocuments().add(document);
        submissionDocumentRepository.save(document);

        return toDocumentResponse(document);
    }

    @Transactional
    public void deleteDocument(Long submissionId, Long documentId, String username) {
        AppUser student = getCurrentUser(username);
        StudentCourseSubmission submission = getOwnedSubmission(submissionId, student.getId());

        ensureEditable(submission);

        SubmissionDocument document = submissionDocumentRepository.findByIdAndSubmissionId(documentId, submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden."));

        submission.getDocuments().remove(document);
        submissionDocumentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public SubmissionDocumentDownloadData downloadDocument(Long submissionId, Long documentId, String username) {
        AppUser student = getCurrentUser(username);
        getOwnedSubmission(submissionId, student.getId());

        SubmissionDocument document = submissionDocumentRepository.findByIdAndSubmissionId(documentId, submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nicht gefunden."));

        byte[] content;
        try {
            content = Base64.getDecoder().decode(document.getContentBase64());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dokumentinhalt ist ungültig.");
        }

        return new SubmissionDocumentDownloadData(
                document.getFileName(),
                document.getMimeType(),
                document.getFileSize(),
                content
        );
    }

    @Transactional
    public void submitSubmission(Long submissionId, String username) {
        AppUser student = getCurrentUser(username);
        StudentCourseSubmission submission = getOwnedSubmission(submissionId, student.getId());

        ensureEditable(submission);

        if (submission.getDocuments() == null || submission.getDocuments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens ein Dokument muss hochgeladen werden.");
        }

        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmissionDate(LocalDateTime.now());

        submissionRepository.save(submission);
    }

    private AppUser getCurrentUser(String username) {
        return appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden."));
    }

    private StudentCourseSubmission getOwnedSubmission(Long submissionId, Long studentId) {
        return submissionRepository.findByIdAndStudentId(submissionId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Abgabe nicht gefunden."));
    }

    private void ensureEditable(StudentCourseSubmission submission) {
        if (!isEditable(submission)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese Abgabe kann nicht mehr bearbeitet werden."
            );
        }
    }

    private boolean isEditable(StudentCourseSubmission submission) {
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            return false;
        }

        CourseSeries courseSeries = submission.getCourseSeries();
        LocalDateTime now = LocalDateTime.now();

        if (courseSeries.getSubmissionStartDate() != null && now.isBefore(courseSeries.getSubmissionStartDate())) {
            return false;
        }

        LocalDateTime effectiveDeadline = resolveEffectiveDeadline(submission);

        if (effectiveDeadline == null) {
            return true;
        }

        if (!now.isAfter(effectiveDeadline)) {
            return true;
        }

        return submission.isLateSubmissionAllowed();
    }

    private boolean isOverdue(StudentCourseSubmission submission) {
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            return false;
        }

        LocalDateTime effectiveDeadline = resolveEffectiveDeadline(submission);

        if (effectiveDeadline == null) {
            return false;
        }

        if (!LocalDateTime.now().isAfter(effectiveDeadline)) {
            return false;
        }

        return !submission.isLateSubmissionAllowed();
    }

    private void validateUploadRequest(UploadSubmissionDocumentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request fehlt.");
        }

        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dateiname fehlt.");
        }

        if (request.mimeType() == null || request.mimeType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIME-Type fehlt.");
        }

        if (!ALLOWED_MIME_TYPES.contains(request.mimeType().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIME-Type nicht erlaubt.");
        }

        validateFileExtension(request.fileName().trim());

        if (request.contentBase64() == null || request.contentBase64().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dateiinhalt fehlt.");
        }
    }

    private void validateFileExtension(String fileName) {
        String lower = fileName.toLowerCase();

        boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dateiendung nicht erlaubt.");
        }
    }

    private StudentSubmissionListItemResponse toListItemResponse(StudentCourseSubmission submission) {
        boolean hasDocuments = submission.getDocuments() != null && !submission.getDocuments().isEmpty();
        LocalDateTime effectiveDeadline = resolveEffectiveDeadline(submission);
        boolean hasIndividualDeadline = hasIndividualDeadline(submission);

        return new StudentSubmissionListItemResponse(
                submission.getId(),
                submission.getCourseSeries().getId(),
                resolveCourseName(submission),
                resolveStudyGroupNames(submission),
                submission.getCourseSeries().getSelectedExamType() != null
                        ? submission.getCourseSeries().getSelectedExamType().getNameDe()
                        : null,
                submission.getStatus(),
                submission.getCourseSeries().getSubmissionStartDate(),
                submission.getCourseSeries().getSubmissionDeadline(),
                effectiveDeadline,
                submission.getExtendedUntil(),
                hasIndividualDeadline,
                hasDocuments,
                !hasDocuments,
                isEditable(submission),
                isOverdue(submission),
                submission.getGrade(),
                submission.getPoints()
        );
    }

    private StudentSubmissionDetailResponse toDetailResponse(StudentCourseSubmission submission) {
        boolean hasDocuments = submission.getDocuments() != null && !submission.getDocuments().isEmpty();
        LocalDateTime effectiveDeadline = resolveEffectiveDeadline(submission);
        boolean hasIndividualDeadline = hasIndividualDeadline(submission);

        List<SubmissionDocumentResponse> documents = submission.getDocuments() == null
                ? List.of()
                : submission.getDocuments()
                  .stream()
                  .sorted(Comparator.comparing(SubmissionDocument::getUploadedAt).reversed())
                  .map(this::toDocumentResponse)
                  .toList();

        return new StudentSubmissionDetailResponse(
                submission.getId(),
                submission.getCourseSeries().getId(),
                resolveCourseName(submission),
                resolveStudyGroupNames(submission),
                submission.getCourseSeries().getSelectedExamType() != null
                        ? submission.getCourseSeries().getSelectedExamType().getNameDe()
                        : null,
                submission.getStatus(),
                submission.getCourseSeries().getSubmissionStartDate(),
                submission.getCourseSeries().getSubmissionDeadline(),
                effectiveDeadline,
                submission.getExtendedUntil(),
                hasIndividualDeadline,
                submission.getSubmissionDate(),
                hasDocuments,
                !hasDocuments,
                isEditable(submission),
                isEditable(submission) && hasDocuments,
                submission.getGrade(),
                submission.getPoints(),
                submission.getFeedback(),
                documents
        );
    }

    private SubmissionDocumentResponse toDocumentResponse(SubmissionDocument document) {
        return new SubmissionDocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getMimeType(),
                document.getFileSize(),
                document.getUploadedAt()
        );
    }

    private String resolveCourseName(StudentCourseSubmission submission) {
        if (submission.getCourseSeries() == null || submission.getCourseSeries().getModule() == null) {
            return null;
        }
        return submission.getCourseSeries().getModule().getName();
    }

    private List<String> resolveStudyGroupNames(StudentCourseSubmission submission) {
        if (submission.getCourseSeries() == null || submission.getCourseSeries().getStudyGroups() == null) {
            return List.of();
        }

        return submission.getCourseSeries().getStudyGroups()
                .stream()
                .map(StudyGroup::getName)
                .sorted()
                .toList();
    }

    private LocalDateTime resolveEffectiveDeadline(StudentCourseSubmission submission) {
        if (submission.getExtendedUntil() != null) {
            return submission.getExtendedUntil();
        }
        return submission.getCourseSeries().getSubmissionDeadline();
    }

    private boolean hasIndividualDeadline(StudentCourseSubmission submission) {
        return submission.getExtendedUntil() != null;
    }
}
