package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.NotificationDto;
import de.campusplatform.campus_platform_service.dto.StudentNotificationResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Notification;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String GRADE_TYPE = "GRADE";

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public void createOrRefreshGradeNotification(AppUser student, StudentCourseSubmission submission) {
        String referenceKey = "GRADE:" + submission.getId();
        String moduleName = submission.getCourseSeries() != null && submission.getCourseSeries().getModule() != null
                ? submission.getCourseSeries().getModule().getName()
                : "Unbekannter Kurs";

        Notification notification = notificationRepository
                .findByUserIdAndTypeAndReferenceKey(student.getId(), GRADE_TYPE, referenceKey)
                .orElseGet(() -> Notification.builder()
                        .user(student)
                        .type(GRADE_TYPE)
                        .referenceKey(referenceKey)
                        .build());

        notification.setIcon("grade");
        notification.setColorClass("success");
        notification.setText("Note " + submission.getGrade() + " in <strong>" + moduleName + "</strong> eingetragen");
        notification.setDetail(null);
        notification.setReadAt(null);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markAllAsRead(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        List<Notification> unread = notificationRepository.findByUserIdAndReadAtIsNull(user.getId());
        if (unread.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(item -> item.setReadAt(now));
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public List<StudentNotificationResponse> getRecentStudentNotifications(Long userId) {
        return notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new StudentNotificationResponse(
                        n.getType(),
                        n.getIcon(),
                        n.getColorClass(),
                        n.getText(),
                        n.getDetail()
                ))
                .toList();
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getIcon(),
                notification.getColorClass(),
                notification.getText(),
                notification.getDetail(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
