package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.SubmissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionDocumentRepository extends JpaRepository<SubmissionDocument, Long> {

    Optional<SubmissionDocument> findByIdAndSubmissionId(Long documentId, Long submissionId);
}
