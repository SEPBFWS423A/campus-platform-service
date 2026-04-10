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
import de.campusplatform.campus_platform_service.model.SubmissionDocument;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.StudentCourseSubmissionRepository;
import de.campusplatform.campus_platform_service.repository.SubmissionDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private static final Map<String, String> EXTENSION_TO_MIME_TYPE = Map.of(
            ".pdf", "application/pdf",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private final StudentCourseSubmissionRepository submissionRepository;
    private final SubmissionDocumentRepository submissionDocumentRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<StudentSubmissionListItemResponse> getMySubmissions(String username) {
        AppUser student = getCurrentUser(username);

        return submissionRepository.findByStudentId(student.getId())
                .stream()
                .sorted(Comparator.comparing(
                        (StudentCourseSubmission s) -> s.getCourseSeries().getSubmissionDeadline(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toListItemResponse)
                .toList();
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

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_request");
        }

        String fileName = validateAndNormalizeFileName(request.fileName());
        String mimeType = validateAndNormalizeMimeType(request.mimeType());
        String base64Content = validateAndNormalizeBase64(request.contentBase64());

        byte[] decodedContent;
        try {
            decodedContent = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        validateFileSize(request.fileSize(), decodedContent.length);

        String extension = extractExtension(fileName);
        validateExtension(extension);
        validateMimeTypeMatchesExtension(mimeType, extension);
        validateActualFileContent(decodedContent, extension);

        SubmissionDocument document = SubmissionDocument.builder()
                .submission(submission)
                .fileName(fileName)
                .mimeType(mimeType)
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.document_not_found"));

        submission.getDocuments().remove(document);
        submissionDocumentRepository.delete(document);
    }

    @Transactional
    public void submitSubmission(Long submissionId, String username) {
        AppUser student = getCurrentUser(username);
        StudentCourseSubmission submission = getOwnedSubmission(submissionId, student.getId());

        ensureEditable(submission);

        if (submission.getDocuments() == null || submission.getDocuments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.submission_no_document");
        }

        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmissionDate(LocalDateTime.now());

        submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionDocumentDownloadData downloadDocument(Long submissionId, Long documentId, String username) {
        AppUser student = getCurrentUser(username);

        getOwnedSubmission(submissionId, student.getId());

        SubmissionDocument document = submissionDocumentRepository.findByIdAndSubmissionId(documentId, submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.document_not_found"));

        byte[] decodedContent;
        try {
            decodedContent = Base64.getDecoder().decode(document.getContentBase64());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        return new SubmissionDocumentDownloadData(
                document.getFileName(),
                document.getMimeType(),
                document.getFileSize() != null ? document.getFileSize() : decodedContent.length,
                decodedContent
        );
    }

    private AppUser getCurrentUser(String username) {
        return appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.user_not_found"));
    }

    private StudentCourseSubmission getOwnedSubmission(Long submissionId, Long studentId) {
        return submissionRepository.findByIdAndStudentId(submissionId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.submission_not_found"));
    }

    private void ensureEditable(StudentCourseSubmission submission) {
        if (!isEditable(submission)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "error.submission_not_editable");
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

        return courseSeries.getSubmissionDeadline() == null || !now.isAfter(courseSeries.getSubmissionDeadline());
    }

    private boolean isOverdue(StudentCourseSubmission submission) {
        CourseSeries courseSeries = submission.getCourseSeries();
        LocalDateTime deadline = courseSeries.getSubmissionDeadline();

        return deadline != null
                && LocalDateTime.now().isAfter(deadline)
                && submission.getStatus() == SubmissionStatus.PENDING;
    }

    private String validateAndNormalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_name");
        }

        String sanitized = fileName.trim();

        if (sanitized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_name");
        }

        if (sanitized.contains("..")
                || sanitized.contains("/")
                || sanitized.contains("\\")
                || sanitized.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_name");
        }

        return sanitized;
    }

    private String validateAndNormalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }

        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);

        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }

        if (!ALLOWED_MIME_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }

        return normalized;
    }

    private String validateAndNormalizeBase64(String contentBase64) {
        if (contentBase64 == null || contentBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        String normalized = contentBase64.trim();

        if (normalized.startsWith("data:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        return normalized;
    }

    private void validateFileSize(Long requestFileSize, int decodedLength) {
        if (decodedLength <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        if (decodedLength > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.file_too_large");
        }

        if (requestFileSize != null && !requestFileSize.equals((long) decodedLength)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }

        return fileName.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }
    }

    private void validateMimeTypeMatchesExtension(String mimeType, String extension) {
        String expectedMimeType = EXTENSION_TO_MIME_TYPE.get(extension);

        if (expectedMimeType == null || !expectedMimeType.equals(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }
    }

    private void validateActualFileContent(byte[] fileContent, String extension) {
        switch (extension) {
            case ".pdf" -> validatePdfContent(fileContent);
            case ".xlsx" -> validateOfficeZipContent(fileContent, "xl/");
            case ".pptx" -> validateOfficeZipContent(fileContent, "ppt/");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_type");
        }
    }

    private void validatePdfContent(byte[] fileContent) {
        if (fileContent.length < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        boolean isPdf = fileContent[0] == '%'
                && fileContent[1] == 'P'
                && fileContent[2] == 'D'
                && fileContent[3] == 'F'
                && fileContent[4] == '-';

        if (!isPdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }
    }

    private void validateOfficeZipContent(byte[] fileContent, String requiredFolderPrefix) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileContent))) {
            boolean hasContentType = false;
            boolean hasRequiredFolder = false;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if ("[Content_Types].xml".equals(name)) {
                    hasContentType = true;
                }

                if (name.startsWith(requiredFolderPrefix)) {
                    hasRequiredFolder = true;
                }

                if (hasContentType && hasRequiredFolder) {
                    return;
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.invalid_file_content");
    }

    private StudentSubmissionListItemResponse toListItemResponse(StudentCourseSubmission submission) {
        boolean hasDocuments = submission.getDocuments() != null && !submission.getDocuments().isEmpty();

        return new StudentSubmissionListItemResponse(
                submission.getId(),
                submission.getCourseSeries().getId(),
                submission.getCourseSeries().getSelectedExamType() != null
                        ? submission.getCourseSeries().getSelectedExamType().getNameDe()
                        : null,
                submission.getStatus(),
                submission.getCourseSeries().getSubmissionStartDate(),
                submission.getCourseSeries().getSubmissionDeadline(),
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

        List<SubmissionDocumentResponse> documents =
                submission.getDocuments() == null
                        ? List.of()
                        : submission.getDocuments().stream()
                          .sorted(Comparator.comparing(SubmissionDocument::getUploadedAt).reversed())
                          .map(this::toDocumentResponse)
                          .toList();

        return new StudentSubmissionDetailResponse(
                submission.getId(),
                submission.getCourseSeries().getId(),
                submission.getCourseSeries().getSelectedExamType() != null
                        ? submission.getCourseSeries().getSelectedExamType().getNameDe()
                        : null,
                submission.getStatus(),
                submission.getCourseSeries().getSubmissionStartDate(),
                submission.getCourseSeries().getSubmissionDeadline(),
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
}
