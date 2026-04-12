package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CourseSeriesRepository extends JpaRepository<CourseSeries, Long> {
    List<CourseSeries> findByAssignedLecturerId(Long lecturerId);
    List<CourseSeries> findByAssignedLecturerIdAndStatusNot(Long lecturerId, de.campusplatform.campus_platform_service.enums.CourseStatus status);
    List<CourseSeries> findDistinctByModuleIdAndStudyGroups_IdIn(Long moduleId, Collection<Long> studyGroupIds);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT cs FROM CourseSeries cs " +
           "JOIN cs.studyGroups sg " +
           "JOIN sg.memberships m " +
           "WHERE m.student.userId = :userId " +
           "AND cs.status != de.campusplatform.campus_platform_service.enums.CourseStatus.PLANNED")
    List<CourseSeries> findNonPlannedCourseSeriesByStudentUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
