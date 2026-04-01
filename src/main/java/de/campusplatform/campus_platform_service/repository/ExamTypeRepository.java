package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamType, Long> {
    Optional<ExamType> findByType(String type);
}
