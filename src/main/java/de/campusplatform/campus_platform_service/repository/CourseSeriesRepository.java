package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseSeries;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourseSeriesRepository extends JpaRepository<CourseSeries, Long> {
    List<CourseSeries> findByAssignedLecturerId(Long lecturerId);
    List<CourseSeries> findDistinctByModuleIdAndStudyGroups_IdIn(Long moduleId, Collection<Long> studyGroupIds);

    @EntityGraph(attributePaths = {
            "module",
            "module.courseOfStudy",
            "selectedExamType",
            "events",
            "studyGroups"
    })
    @Query("""
    select distinct cs
    from CourseSeries cs
    join cs.studyGroups sg
    where sg.id in :studyGroupIds
""")
    List<CourseSeries> findVisibleForStudentGradeOverview(@Param("studyGroupIds") Collection<Long> studyGroupIds);
}
