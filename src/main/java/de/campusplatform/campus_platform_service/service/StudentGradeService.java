package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AcademicTermResponse;
import de.campusplatform.campus_platform_service.dto.AssessmentTypeResponse;
import de.campusplatform.campus_platform_service.dto.StudentGradeOverviewItemResponse;
import de.campusplatform.campus_platform_service.dto.StudentGradeOverviewResponse;
import de.campusplatform.campus_platform_service.dto.StudentGradeSemesterGroupResponse;
import de.campusplatform.campus_platform_service.dto.StudentGradeSummaryResponse;
import de.campusplatform.campus_platform_service.enums.AcademicTermSeason;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.enums.StudentGradeStatus;
import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.CourseOfStudy;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.ExamType;
import de.campusplatform.campus_platform_service.model.Module;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import de.campusplatform.campus_platform_service.model.StudentProfile;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.ModuleRepository;
import de.campusplatform.campus_platform_service.repository.StudentCourseSubmissionRepository;
import de.campusplatform.campus_platform_service.repository.StudentProfileRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class StudentGradeService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudyGroupMembershipRepository studyGroupMembershipRepository;
    private final CourseSeriesRepository courseSeriesRepository;
    private final StudentCourseSubmissionRepository submissionRepository;
    private final ModuleRepository moduleRepository;

    public StudentGradeService(StudentProfileRepository studentProfileRepository,
                               StudyGroupMembershipRepository studyGroupMembershipRepository,
                               CourseSeriesRepository courseSeriesRepository,
                               StudentCourseSubmissionRepository submissionRepository,
                               ModuleRepository moduleRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.studyGroupMembershipRepository = studyGroupMembershipRepository;
        this.courseSeriesRepository = courseSeriesRepository;
        this.submissionRepository = submissionRepository;
        this.moduleRepository = moduleRepository;
    }

    public StudentGradeOverviewResponse getOverviewForStudent(Long studentId) {
        StudentProfile studentProfile = studentProfileRepository.findByAppUserId(studentId)
                .orElseThrow(() -> new AppException("error.student.notFound"));

        List<Long> ownStudyGroupIds = studyGroupMembershipRepository.findStudyGroupIdsByStudentUserId(studentId);
        if (ownStudyGroupIds.isEmpty()) {
            return emptyResponse();
        }

        List<CourseSeries> visibleCourseSeries = courseSeriesRepository.findVisibleForStudentGradeOverview(ownStudyGroupIds);
        if (visibleCourseSeries.isEmpty()) {
            return emptyResponse();
        }

        List<Long> courseSeriesIds = visibleCourseSeries.stream()
                .map(CourseSeries::getId)
                .toList();

        List<StudentCourseSubmission> ownSubmissions =
                submissionRepository.findOwnOverviewSubmissions(studentId, courseSeriesIds);

        Map<Long, StudentCourseSubmission> latestOwnSubmissionByCourseSeries = ownSubmissions.stream()
                .collect(Collectors.toMap(
                        submission -> submission.getCourseSeries().getId(),
                        Function.identity(),
                        this::pickNewerSubmission,
                        HashMap::new
                ));

        Set<Long> ownStudyGroupIdSet = new HashSet<>(ownStudyGroupIds);

        List<StudentGradeOverviewItemResponse> rawItems = visibleCourseSeries.stream()
                .map(courseSeries -> toOverviewItem(
                        courseSeries,
                        latestOwnSubmissionByCourseSeries.get(courseSeries.getId()),
                        ownStudyGroupIdSet
                ))
                .toList();

        // 1) Anzeige: ALLE zugeordneten Kurse / Module anzeigen
        List<StudentGradeOverviewItemResponse> displayItems = rawItems.stream()
                .sorted(Comparator
                        .comparing(StudentGradeOverviewItemResponse::getModuleSemester, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(StudentGradeOverviewItemResponse::getExamDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(StudentGradeOverviewItemResponse::getModuleName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(StudentGradeOverviewItemResponse::getCourseSeriesId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        // 2) Summary: pro Modul nur den neuesten Versuch berücksichtigen
        List<StudentGradeOverviewItemResponse> summaryItems = rawItems.stream()
                .collect(Collectors.toMap(
                        StudentGradeOverviewItemResponse::getModuleId,
                        Function.identity(),
                        this::pickLatestModuleItem,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        StudentGradeSummaryResponse summary = buildSummary(summaryItems, studentProfile, visibleCourseSeries);

        List<StudentGradeSemesterGroupResponse> semesters = displayItems.stream()
                .collect(Collectors.groupingBy(
                        StudentGradeOverviewItemResponse::getModuleSemester,
                        TreeMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> StudentGradeSemesterGroupResponse.builder()
                        .moduleSemester(entry.getKey())
                        .items(entry.getValue())
                        .build())
                .toList();

        return StudentGradeOverviewResponse.builder()
                .summary(summary)
                .semesters(semesters)
                .build();
    }

    private StudentGradeOverviewItemResponse toOverviewItem(
            CourseSeries courseSeries,
            StudentCourseSubmission submission,
            Set<Long> ownStudyGroupIds
    ) {
        Module courseModule = courseSeries.getModule();

        LocalDate examDate = resolveExamDate(courseSeries);
        LocalDateTime termReferenceDate = resolveTermReferenceDate(courseSeries, submission);

        List<String> ownStudyGroupNamesForSeries = courseSeries.getStudyGroups().stream()
                .filter(group -> ownStudyGroupIds.contains(group.getId()))
                .map(StudyGroup::getName)
                .sorted()
                .toList();

        return StudentGradeOverviewItemResponse.builder()
                .courseSeriesId(courseSeries.getId())
                .moduleId(courseModule.getId())
                .moduleName(courseModule.getName())
                .moduleSemester(courseModule.getSemester())
                .studyGroupNames(ownStudyGroupNamesForSeries)
                .assessmentType(toAssessmentType(courseSeries.getSelectedExamType()))
                .examDate(examDate)
                .academicTerm(deriveAcademicTerm(termReferenceDate))
                .ects(courseModule.getEcts())
                .status(determineStatus(submission))
                .attemptNumber(submission != null && submission.getAttemptNumber() != null ? submission.getAttemptNumber() : 1)
                .grade(isRegularGradedSubmission(submission) ? submission.getGrade() : null)
                .reviewerComment(extractVisibleFeedback(submission))
                .lastUpdatedAt(submission != null ? submission.getUpdatedAt() : null)
                .build();
    }

    private AssessmentTypeResponse toAssessmentType(ExamType examType) {
        if (examType == null) {
            return null;
        }

        return AssessmentTypeResponse.builder()
                .code(examType.getType())
                .submission(examType.isSubmission())
                .nameDe(examType.getNameDe())
                .nameEn(examType.getNameEn())
                .shortDe(examType.getShortDe())
                .shortEn(examType.getShortEn())
                .build();
    }

    private StudentGradeStatus determineStatus(StudentCourseSubmission submission) {
        if (submission == null) {
            return StudentGradeStatus.PENDING;
        }

        SubmissionStatus rawStatus = submission.getStatus();

        if (rawStatus == SubmissionStatus.EXCUSED_ABSENCE) {
            return StudentGradeStatus.EXCUSED_ABSENCE;
        }

        if (rawStatus == SubmissionStatus.UNEXCUSED_ABSENCE) {
            return StudentGradeStatus.UNEXCUSED_ABSENCE;
        }

        if (rawStatus == SubmissionStatus.EXCLUDED) {
            return StudentGradeStatus.EXCLUDED;
        }

        if (submission.getGrade() == null) {
            return StudentGradeStatus.PENDING;
        }

        return submission.getGrade() <= 4.0
                ? StudentGradeStatus.PASSED
                : StudentGradeStatus.FAILED;
    }

    private boolean isRegularGradedSubmission(StudentCourseSubmission submission) {
        if (submission == null) {
            return false;
        }

        SubmissionStatus status = submission.getStatus();
        return status != SubmissionStatus.EXCUSED_ABSENCE
                && status != SubmissionStatus.UNEXCUSED_ABSENCE
                && status != SubmissionStatus.EXCLUDED
                && submission.getGrade() != null;
    }

    private String extractVisibleFeedback(StudentCourseSubmission submission) {
        if (submission == null || submission.getFeedback() == null || submission.getFeedback().isBlank()) {
            return null;
        }
        return submission.getFeedback();
    }

    private LocalDate resolveExamDate(CourseSeries courseSeries) {
        return getRelevantEventsForDate(courseSeries)
                .map(Event::getStartTime)
                .filter(Objects::nonNull)
                .sorted()
                .map(LocalDateTime::toLocalDate)
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime resolveTermReferenceDate(CourseSeries courseSeries, StudentCourseSubmission submission) {
        LocalDateTime eventDate = getRelevantEventsForDate(courseSeries)
                .map(Event::getStartTime)
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);

        if (eventDate != null) {
            return eventDate;
        }
        if (submission != null && submission.getSubmissionDate() != null) {
            return submission.getSubmissionDate();
        }
        if (courseSeries.getSubmissionDeadline() != null) {
            return courseSeries.getSubmissionDeadline();
        }
        return courseSeries.getSubmissionStartDate();
    }

    private Stream<Event> getRelevantEventsForDate(CourseSeries courseSeries) {
        List<Event> klausurEvents = courseSeries.getEvents().stream()
                .filter(event -> event.getEventType() == EventType.KLAUSUR)
                .toList();

        if (!klausurEvents.isEmpty()) {
            return klausurEvents.stream();
        }

        return courseSeries.getEvents().stream();
    }

    private AcademicTermResponse deriveAcademicTerm(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();

        if (month >= 4 && month <= 9) {
            return AcademicTermResponse.builder()
                    .season(AcademicTermSeason.SUMMER)
                    .startYear(year)
                    .endYear(null)
                    .build();
        }

        int winterStartYear = month >= 10 ? year : year - 1;

        return AcademicTermResponse.builder()
                .season(AcademicTermSeason.WINTER)
                .startYear(winterStartYear)
                .endYear(winterStartYear + 1)
                .build();
    }

    private StudentCourseSubmission pickNewerSubmission(StudentCourseSubmission left, StudentCourseSubmission right) {
        int leftAttempt = left.getAttemptNumber() != null ? left.getAttemptNumber() : 1;
        int rightAttempt = right.getAttemptNumber() != null ? right.getAttemptNumber() : 1;

        if (leftAttempt != rightAttempt) {
            return leftAttempt > rightAttempt ? left : right;
        }

        LocalDateTime leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt() : left.getSubmissionDate();
        LocalDateTime rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt() : right.getSubmissionDate();

        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }

        return leftTime.isAfter(rightTime) ? left : right;
    }

    private StudentGradeOverviewItemResponse pickLatestModuleItem(
            StudentGradeOverviewItemResponse left,
            StudentGradeOverviewItemResponse right
    ) {
        int leftAttempt = left.getAttemptNumber() != null ? left.getAttemptNumber() : 1;
        int rightAttempt = right.getAttemptNumber() != null ? right.getAttemptNumber() : 1;

        if (leftAttempt != rightAttempt) {
            return leftAttempt > rightAttempt ? left : right;
        }

        LocalDateTime leftUpdated = left.getLastUpdatedAt();
        LocalDateTime rightUpdated = right.getLastUpdatedAt();

        if (leftUpdated != null && rightUpdated != null && !leftUpdated.equals(rightUpdated)) {
            return leftUpdated.isAfter(rightUpdated) ? left : right;
        }

        LocalDate leftExamDate = left.getExamDate();
        LocalDate rightExamDate = right.getExamDate();

        if (leftExamDate != null && rightExamDate != null && !leftExamDate.equals(rightExamDate)) {
            return leftExamDate.isAfter(rightExamDate) ? left : right;
        }

        if (leftUpdated == null && rightUpdated != null) {
            return right;
        }
        if (leftUpdated != null && rightUpdated == null) {
            return left;
        }

        if (Objects.equals(left.getCourseSeriesId(), right.getCourseSeriesId())) {
            return left;
        }

        if (left.getCourseSeriesId() == null) {
            return right;
        }
        if (right.getCourseSeriesId() == null) {
            return left;
        }

        return left.getCourseSeriesId() > right.getCourseSeriesId() ? left : right;
    }

    private StudentGradeSummaryResponse buildSummary(
            List<StudentGradeOverviewItemResponse> items,
            StudentProfile studentProfile,
            List<CourseSeries> visibleCourseSeries
    ) {
        int gradedAssessmentsCount = (int) items.stream()
                .filter(this::isFinalizedAssessment)
                .count();

        int pendingAssessmentsCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.PENDING)
                .count();

        int excusedAbsenceAssessmentsCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.EXCUSED_ABSENCE)
                .count();

        int unexcusedAbsenceAssessmentsCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.UNEXCUSED_ABSENCE)
                .count();

        int excludedAssessmentsCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.EXCLUDED)
                .count();

        int passedModulesCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.PASSED)
                .count();

        int failedModulesCount = (int) items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.FAILED)
                .count();

        int achievedEcts = items.stream()
                .filter(item -> item.getStatus() == StudentGradeStatus.PASSED)
                .map(StudentGradeOverviewItemResponse::getEcts)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        Double currentAverage = calculateWeightedAverage(
                items.stream()
                        .filter(this::isNumericGradedItem)
                        .toList()
        );

        Integer totalEcts = resolveTotalEcts(studentProfile, visibleCourseSeries);

        return StudentGradeSummaryResponse.builder()
                .currentAverage(currentAverage)
                .achievedEcts(achievedEcts)
                .totalEcts(totalEcts)
                .passedModulesCount(passedModulesCount)
                .failedModulesCount(failedModulesCount)
                .gradedAssessmentsCount(gradedAssessmentsCount)
                .pendingAssessmentsCount(pendingAssessmentsCount)
                .excusedAbsenceAssessmentsCount(excusedAbsenceAssessmentsCount)
                .unexcusedAbsenceAssessmentsCount(unexcusedAbsenceAssessmentsCount)
                .excludedAssessmentsCount(excludedAssessmentsCount)
                .build();
    }

    private boolean isNumericGradedItem(StudentGradeOverviewItemResponse item) {
        return item.getGrade() != null
                && (item.getStatus() == StudentGradeStatus.PASSED || item.getStatus() == StudentGradeStatus.FAILED);
    }

    private boolean isFinalizedAssessment(StudentGradeOverviewItemResponse item) {
        return item.getStatus() == StudentGradeStatus.PASSED
                || item.getStatus() == StudentGradeStatus.FAILED
                || item.getStatus() == StudentGradeStatus.EXCUSED_ABSENCE
                || item.getStatus() == StudentGradeStatus.UNEXCUSED_ABSENCE
                || item.getStatus() == StudentGradeStatus.EXCLUDED;
    }

    private Double calculateWeightedAverage(Collection<StudentGradeOverviewItemResponse> items) {
        double weightedSum = 0.0;
        int totalWeight = 0;

        for (StudentGradeOverviewItemResponse item : items) {
            if (item.getGrade() == null) {
                continue;
            }

            int weight = item.getEcts() != null && item.getEcts() > 0 ? item.getEcts() : 1;
            weightedSum += item.getGrade() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return null;
        }

        return roundToOneDecimal(weightedSum / totalWeight);
    }

    private Double roundToOneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Integer resolveTotalEcts(StudentProfile studentProfile, List<CourseSeries> visibleCourseSeries) {
        CourseOfStudy courseOfStudy = visibleCourseSeries.stream()
                .map(CourseSeries::getModule)
                .filter(Objects::nonNull)
                .map(Module::getCourseOfStudy)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (courseOfStudy == null) {
            return 0;
        }

        Long specializationId = studentProfile.getSpecialization() != null
                ? studentProfile.getSpecialization().getId()
                : null;

        return moduleRepository.findRelevantForOverview(courseOfStudy.getId(), specializationId).stream()
                .map(Module::getEcts)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private StudentGradeOverviewResponse emptyResponse() {
        return StudentGradeOverviewResponse.builder()
                .summary(StudentGradeSummaryResponse.builder()
                        .currentAverage(null)
                        .achievedEcts(0)
                        .totalEcts(0)
                        .passedModulesCount(0)
                        .failedModulesCount(0)
                        .gradedAssessmentsCount(0)
                        .pendingAssessmentsCount(0)
                        .excusedAbsenceAssessmentsCount(0)
                        .unexcusedAbsenceAssessmentsCount(0)
                        .excludedAssessmentsCount(0)
                        .build())
                .semesters(List.of())
                .build();
    }
}