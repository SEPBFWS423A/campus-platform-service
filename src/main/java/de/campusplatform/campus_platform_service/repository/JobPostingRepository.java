package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.enums.JobStatus;
import de.campusplatform.campus_platform_service.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findAllByOrderByCreatedAtDesc();
    List<JobPosting> findByStatusOrderByCreatedAtDesc(JobStatus status);
}
