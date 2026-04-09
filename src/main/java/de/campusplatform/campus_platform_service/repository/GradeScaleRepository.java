package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.GradeScaleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeScaleRepository extends JpaRepository<GradeScaleEntry, Long> {
    List<GradeScaleEntry> findAllByOrderByMinimumPointsDesc();
}
