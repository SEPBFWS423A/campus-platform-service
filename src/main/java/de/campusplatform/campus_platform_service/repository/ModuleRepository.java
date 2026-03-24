package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseOfStudyId(Long courseOfStudyId);
}
