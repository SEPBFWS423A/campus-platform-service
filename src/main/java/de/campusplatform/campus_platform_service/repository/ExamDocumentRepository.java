package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.ExamDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import de.campusplatform.campus_platform_service.enums.ExamDocumentType;

@Repository
public interface ExamDocumentRepository extends JpaRepository<ExamDocument, Long> {
    Optional<ExamDocument> findByCourseSeriesIdAndType(Long seriesId, ExamDocumentType type);
}
