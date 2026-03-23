package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseOfStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseOfStudyRepository extends JpaRepository<CourseOfStudy, Long> {
    Optional<CourseOfStudy> findByName(String name);
}
