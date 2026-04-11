package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourseSeriesRepository extends JpaRepository<CourseSeries, Long> {
    List<CourseSeries> findByAssignedLecturerId(Long lecturerId);
    List<CourseSeries> findDistinctByModuleIdAndStudyGroups_IdIn(Long moduleId, Collection<Long> studyGroupIds);
}
