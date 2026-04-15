package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByTargetLecturerIdOrderByCreatedAtDesc(Long lecturerId);
}
