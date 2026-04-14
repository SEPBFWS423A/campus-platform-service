package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.StudentDashboardResponse;
import de.campusplatform.campus_platform_service.dto.StudentNotificationResponse;
import de.campusplatform.campus_platform_service.dto.StudentTodayEventResponse;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.StudentCourseSubmissionRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final AppUserRepository userRepository;
    private final StudyGroupMembershipRepository membershipRepository;
    private final EventRepository eventRepository;
    private final StudentCourseSubmissionRepository submissionRepository;
    private final NotificationService notificationService;

    public StudentDashboardResponse getDashboard(String email) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("error.user.notFound"));

        List<Long> groupIds = membershipRepository
            .findByStudentUserId(user.getId()).stream()
            .map(m -> m.getStudyGroup().getId())
            .collect(Collectors.toList());

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1);
        List<StudentTodayEventResponse> todayEvents = groupIds.isEmpty()
            ? List.of()
            : eventRepository.findNonPlannedOverlappingEventsForGroups(groupIds, todayStart, todayEnd)
                .stream()
                .sorted(Comparator.comparing(Event::getStartTime))
                .map(this::mapToTodayEvent)
                .collect(Collectors.toList());

        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(365);
        long upcomingExams = groupIds.isEmpty() ? 0L :
            eventRepository.findNonPlannedOverlappingEventsForGroups(groupIds, now, horizon)
                .stream()
                .filter(e -> e.getEventType() == EventType.KLAUSUR)
                .count();

        List<StudentCourseSubmission> submissions =
            submissionRepository.findByStudentId(user.getId());
        OptionalDouble avg = submissions.stream()
            .filter(s -> s.getGrade() != null)
            .mapToDouble(StudentCourseSubmission::getGrade)
            .average();

        int ectsEarned = submissions.stream()
            .filter(s -> s.getGrade() != null
                      && s.getCourseSeries() != null
                      && s.getCourseSeries().getModule() != null
                      && s.getCourseSeries().getModule().getEcts() != null)
            .mapToInt(s -> s.getCourseSeries().getModule().getEcts())
            .sum();
        boolean hasEctsData = submissions.stream()
            .anyMatch(s -> s.getCourseSeries() != null
                        && s.getCourseSeries().getModule() != null
                        && s.getCourseSeries().getModule().getEcts() != null);

        Integer ectsTotal = null;
        if (user.getStudentProfile() != null
                && user.getStudentProfile().getSpecialization() != null
                && user.getStudentProfile().getSpecialization().getCourseOfStudy() != null) {
            ectsTotal = user.getStudentProfile().getSpecialization()
                            .getCourseOfStudy().getTotalEcts();
        }

        List<StudentNotificationResponse> notifications = notificationService.getRecentStudentNotifications(user.getId());

        String courseOfStudyName = null;
        if (user.getStudentProfile() != null
                && user.getStudentProfile().getSpecialization() != null
                && user.getStudentProfile().getSpecialization().getCourseOfStudy() != null) {
            courseOfStudyName = user.getStudentProfile().getSpecialization()
                                    .getCourseOfStudy().getName();
        }

        return new StudentDashboardResponse(
            user.getFirstName(),
            user.getLastName(),
            courseOfStudyName,
            avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : null,
            hasEctsData ? ectsEarned : null,
            ectsTotal,
            upcomingExams,
            todayEvents,
            notifications
        );
    }

    private StudentTodayEventResponse mapToTodayEvent(Event e) {
        String roomName = e.getRooms().stream()
            .map(Room::getName)
            .findFirst()
            .orElse(null);
        String moduleName = e.getCourseSeries() != null && e.getCourseSeries().getModule() != null
            ? e.getCourseSeries().getModule().getName() : e.getName();
        return new StudentTodayEventResponse(
            e.getId(), e.getName(),
            e.getEventType() != null ? e.getEventType().name() : null,
            moduleName, roomName,
            e.getStartTime(), e.getDurationMinutes()
        );
    }

}
