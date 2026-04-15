package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseDocumentRepository extends JpaRepository<CourseDocument, Long> {
}
