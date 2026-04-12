package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseOfStudyId(Long courseOfStudyId);

    @Query("""
        select m
        from Module m
        where m.courseOfStudy.id = :courseOfStudyId
          and (:specializationId is null or m.specialization is null or m.specialization.id = :specializationId)
    """)
    List<Module> findRelevantForOverview(
            @Param("courseOfStudyId") Long courseOfStudyId,
            @Param("specializationId") Long specializationId
    );
}
