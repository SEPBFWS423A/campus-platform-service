package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByOrderByStartDateAsc();
    List<Holiday> findByStartDateBetween(LocalDate start, LocalDate end);
}