package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.AbsenceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceAuditLogRepository extends JpaRepository<AbsenceAuditLog, Long> {
    List<AbsenceAuditLog> findByAbsenceIdOrderByPerformedAtAsc(Long absenceId);
}
