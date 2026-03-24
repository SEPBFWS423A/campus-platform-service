package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    java.util.Optional<StudyGroup> findByName(String name);
}
