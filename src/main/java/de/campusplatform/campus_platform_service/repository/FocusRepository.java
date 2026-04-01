package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Focus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FocusRepository extends JpaRepository<Focus, Long> {
}
