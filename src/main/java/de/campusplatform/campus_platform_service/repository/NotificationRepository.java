package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

	List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserIdAndReadAtIsNull(Long userId);

	List<Notification> findByUserIdAndReadAtIsNull(Long userId);

	Optional<Notification> findByUserIdAndTypeAndReferenceKey(Long userId, String type, String referenceKey);
}
