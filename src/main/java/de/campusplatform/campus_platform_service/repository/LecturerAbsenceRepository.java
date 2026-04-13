package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.LecturerAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LecturerAbsenceRepository extends JpaRepository<LecturerAbsence, Long> {
    List<LecturerAbsence> findByLecturerId(Long lecturerId);
    List<LecturerAbsence> findAllByOrderByStartDateAsc();
    // Für Terminplanung-Konfliktcheck:
    List<LecturerAbsence> findByLecturerIdAndEndDateAfterAndStartDateBefore(
        Long lecturerId, LocalDateTime start, LocalDateTime end);
}
