package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentId(Long studentId);
    boolean existsByStudentIdAndProgramId(Long studentId, Long programId);
}