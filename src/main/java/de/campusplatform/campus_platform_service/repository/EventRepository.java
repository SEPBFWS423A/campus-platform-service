package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
