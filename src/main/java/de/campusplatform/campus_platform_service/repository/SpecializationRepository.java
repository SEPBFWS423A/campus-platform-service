package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
}
