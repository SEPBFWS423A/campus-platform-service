package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.enums.ExamStatus;
import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LecturerService {

    private final CourseSeriesRepository courseSeriesRepository;
    private final StudentCourseSubmissionRepository submissionRepository;
    private final AppUserRepository userRepository;
    private final ExamDocumentRepository documentRepository;
    private final GradeScaleService gradeScaleService;

    public LecturerService(CourseSeriesRepository courseSeriesRepository,
                           StudentCourseSubmissionRepository submissionRepository,
                           AppUserRepository userRepository,
                           ExamDocumentRepository documentRepository, GradeScaleService gradeScaleService) {
        this.courseSeriesRepository = courseSeriesRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.gradeScaleService = gradeScaleService;
    }

    public List<LecturerCourseResponse> getCoursesForLecturer(Long lecturerId) {
        return courseSeriesRepository.findByAssignedLecturerId(lecturerId).stream()
                .map(this::mapToLecturerCourseResponse)
                .collect(Collectors.toList());
    }

    private LecturerCourseResponse mapToLecturerCourseResponse(CourseSeries cs) {
        List<String> studyGroupNames = cs.getStudyGroups().stream()
                .map(StudyGroup::getName)
                .collect(Collectors.toList());

        List<LecturerCourseResponse.EventResponse> events = cs.getEvents().stream()
                .map(e -> new LecturerCourseResponse.EventResponse(
                        e.getId(),
                        e.getEventType() != null ? e.getEventType().name() : null,
                        e.getStartTime(),
                        e.getStartTime() != null && e.getDurationMinutes() != null 
                            ? e.getStartTime().plusMinutes(e.getDurationMinutes()) 
                            : null,
                        e.getRoom() != null ? e.getRoom().getName() : null,
                        e.getRoom() != null ? e.getRoom().getExamSeats() : null
                ))
                .collect(Collectors.toList());

        String examFileName = cs.getDocuments().stream()
                .filter(d -> d.getType() == de.campusplatform.campus_platform_service.enums.ExamDocumentType.EXAM_PAPER)
                .map(ExamDocument::getFileName)
                .findFirst().orElse(null);

        String solutionFileName = cs.getDocuments().stream()
                .filter(d -> d.getType() == de.campusplatform.campus_platform_service.enums.ExamDocumentType.SAMPLE_SOLUTION)
                .map(ExamDocument::getFileName)
                .findFirst().orElse(null);

        Long submissionCount = submissionRepository.countByCourseSeriesIdAndStatus(cs.getId(), SubmissionStatus.SUBMITTED);

        return new LecturerCourseResponse(
                cs.getId(),
                cs.getModule().getName(),
                studyGroupNames,
                cs.getSelectedExamType() != null ? cs.getSelectedExamType().getNameDe() : null,
                cs.getSelectedExamType() != null ? cs.getSelectedExamType().isSubmission() : false,
                resolveEffectiveStatus(cs),
                examFileName,
                solutionFileName,
                cs.getLecturerNotes(),
                cs.getSubmissionDeadline(),
                events,
                submissionCount
        );
    }

    private ExamStatus resolveEffectiveStatus(CourseSeries cs) {
        ExamStatus current = cs.getExamStatus();
        if (current == ExamStatus.COMPLETED || current == ExamStatus.GRADING) {
            return current;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Check for Submission Exams
        if (cs.getSelectedExamType() != null && cs.getSelectedExamType().isSubmission()) {
            if (cs.getSubmissionDeadline() != null && cs.getSubmissionDeadline().isBefore(now)) {
                return ExamStatus.GRADING;
            }
        }

        // 2. Check for Written Exams
        if (cs.getSelectedExamType() != null && !cs.getSelectedExamType().isSubmission()) {
             boolean allEventsPassed = !cs.getEvents().isEmpty() && cs.getEvents().stream()
                     .allMatch(e -> {
                         LocalDateTime end = e.getStartTime() != null && e.getDurationMinutes() != null 
                                 ? e.getStartTime().plusMinutes(e.getDurationMinutes()) 
                                 : e.getStartTime();
                         return end != null && end.isBefore(now);
                     });
             
             if (allEventsPassed) {
                 return ExamStatus.GRADING;
             }
        }

        return current;
    }

    @Transactional
    public void updateExamMaterials(Long seriesId, ExamMaterialsRequest request) {
        CourseSeries cs = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("error.courseSeries.notFound"));
        
        if (cs.getSelectedExamType() == null || cs.getSelectedExamType().isSubmission()) {
            throw new AppException("Only written exams support material uploads");
        }

        if (cs.getExamStatus() != ExamStatus.OPEN && cs.getExamStatus() != ExamStatus.PROVIDED) {
            throw new AppException("Materials can only be uploaded when the exam status is OPEN or PROVIDED.");
        }

        // Handle Exam Paper (Update, Create or Delete)
        if (request.examFileName() == null || request.examFileName().trim().isEmpty()) {
            documentRepository.findByCourseSeriesIdAndType(cs.getId(), de.campusplatform.campus_platform_service.enums.ExamDocumentType.EXAM_PAPER)
                    .ifPresent(documentRepository::delete);
        } else if (request.examContent() != null && !request.examContent().trim().isEmpty()) {
            updateDocument(cs, de.campusplatform.campus_platform_service.enums.ExamDocumentType.EXAM_PAPER, request.examFileName(), request.examContent());
        }

        // Handle Sample Solution (Update, Create or Delete)
        if (request.solutionFileName() == null || request.solutionFileName().trim().isEmpty()) {
            documentRepository.findByCourseSeriesIdAndType(cs.getId(), de.campusplatform.campus_platform_service.enums.ExamDocumentType.SAMPLE_SOLUTION)
                    .ifPresent(documentRepository::delete);
        } else if (request.solutionContent() != null && !request.solutionContent().trim().isEmpty()) {
            updateDocument(cs, de.campusplatform.campus_platform_service.enums.ExamDocumentType.SAMPLE_SOLUTION, request.solutionFileName(), request.solutionContent());
        }

        cs.setLecturerNotes(request.lecturerNotes());
        cs.setExamStatus(ExamStatus.PROVIDED);
        courseSeriesRepository.save(cs);
    }

    private void updateDocument(CourseSeries cs, de.campusplatform.campus_platform_service.enums.ExamDocumentType type, String fileName, String content) {
        ExamDocument doc = documentRepository.findByCourseSeriesIdAndType(cs.getId(), type)
                .orElseGet(() -> ExamDocument.builder()
                        .courseSeries(cs)
                        .type(type)
                        .build());
        doc.setFileName(fileName);
        doc.setContent(content);
        documentRepository.save(doc);
    }

    public List<StudentSubmissionResponse> getSubmissionsForSeries(Long seriesId) {
        // Fetch all students in the groups for this series efficiently
        List<AppUser> students = userRepository.findStudentsByCourseSeriesId(seriesId);

        // Get existing submissions
        Map<Long, StudentCourseSubmission> submissionMap = submissionRepository.findByCourseSeriesId(seriesId).stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        return students.stream()
                .map(student -> {
                    StudentCourseSubmission sub = submissionMap.get(student.getId());
                    return new StudentSubmissionResponse(
                            student.getId(),
                            student.getFirstName() + " " + student.getLastName(),
                            student.getStudentProfile() != null ? student.getStudentProfile().getStudentNumber() : null,
                            sub != null ? sub.getStatus() : SubmissionStatus.PENDING,
                            sub != null ? resolveDocumentValue(sub) : null,                            sub != null ? sub.getSubmissionDate() : null,
                            sub != null ? sub.getGrade() : null,
                            sub != null ? sub.getPoints() : null,
                            sub != null ? sub.getFeedback() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void bulkApplyGrades(Long seriesId, GradeBulkRequest request) {
        CourseSeries cs = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("error.courseSeries.notFound"));

        for (GradeBulkRequest.StudentGradeItem item : request.grades()) {
            StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(seriesId, item.studentId())
                    .orElseGet(() -> {
                        AppUser student = userRepository.findById(item.studentId())
                                .orElseThrow(() -> new AppException("error.student.notFound"));
                        return StudentCourseSubmission.builder()
                                .courseSeries(cs)
                                .student(student)
                                .status(SubmissionStatus.PENDING)
                                .build();
                    });
            
            Double finalGrade = item.grade();
            if (item.points() != null) {
                finalGrade = gradeScaleService.calculateGrade(item.points());
            }

            submission.setGrade(finalGrade);
            submission.setPoints(item.points());
            submission.setFeedback(item.feedback());
            if (submission.getStatus() == SubmissionStatus.PENDING) {
                submission.setStatus(SubmissionStatus.GRADED);
            }
            submissionRepository.save(submission);
        }

        if (cs.getExamStatus() != ExamStatus.COMPLETED) {
            cs.setExamStatus(ExamStatus.GRADING);
            courseSeriesRepository.save(cs);
        }
    }

    @Transactional
    public void publishGrades(Long seriesId) {
        CourseSeries cs = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("error.courseSeries.notFound"));
        
        cs.setExamStatus(ExamStatus.COMPLETED);
        courseSeriesRepository.save(cs);
    }
    @Transactional(readOnly = true)
    public ExamDocumentResponse getExamDocument(Long seriesId, de.campusplatform.campus_platform_service.enums.ExamDocumentType type) {
        ExamDocument doc = documentRepository.findByCourseSeriesIdAndType(seriesId, type)
                .orElseThrow(() -> new AppException("Document not found"));
        return new ExamDocumentResponse(doc.getFileName(), doc.getContent());
    }

    @Transactional
    public StudentSubmissionResponse applySingleGrade(Long seriesId, SingleGradeRequest item) {
        CourseSeries cs = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("error.courseSeries.notFound"));

        StudentCourseSubmission submission = submissionRepository.findByCourseSeriesIdAndStudentId(seriesId, item.studentId())
                .orElseGet(() -> {
                    AppUser student = userRepository.findById(item.studentId())
                            .orElseThrow(() -> new AppException("error.student.notFound"));
                    return StudentCourseSubmission.builder()
                            .courseSeries(cs)
                            .student(student)
                            .status(SubmissionStatus.PENDING)
                            .build();
                });

        Double finalGrade = item.grade();
        if (item.points() != null) {
            finalGrade = gradeScaleService.calculateGrade(item.points());
        }

        submission.setGrade(finalGrade);
        submission.setPoints(item.points());
        submission.setFeedback(item.feedback());

        if (submission.getStatus() == SubmissionStatus.PENDING && (finalGrade != null || item.points() != null)) {
            submission.setStatus(SubmissionStatus.GRADED);
        }
        StudentCourseSubmission saved = submissionRepository.save(submission);

        if (cs.getExamStatus() != ExamStatus.COMPLETED && cs.getExamStatus() != ExamStatus.GRADING) {
            cs.setExamStatus(ExamStatus.GRADING);
            courseSeriesRepository.save(cs);
        }

        return new StudentSubmissionResponse(
                saved.getStudent().getId(),
                saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName(),
                saved.getStudent().getStudentProfile() != null ? saved.getStudent().getStudentProfile().getStudentNumber() : null,
                saved.getStatus(),
                resolveDocumentValue(saved),
                saved.getSubmissionDate(),
                saved.getGrade(),
                saved.getPoints(),
                saved.getFeedback()
        );
    }

    private String resolveDocumentValue(StudentCourseSubmission submission) {
        if (submission == null || submission.getDocuments() == null || submission.getDocuments().isEmpty()) {
            return null;
        }

        return submission.getDocuments().stream()
                .sorted(java.util.Comparator.comparing(
                        SubmissionDocument::getUploadedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                ).reversed())
                .map(SubmissionDocument::getFileName)
                .findFirst()
                .orElse(null);
    }
}
