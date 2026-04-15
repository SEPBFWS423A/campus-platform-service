package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CourseSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSeriesRepository extends JpaRepository<CourseSeries, Long> {
    List<CourseSeries> findByAssignedLecturerId(Long lecturerId);

    @org.springframework.data.jpa.repository.Query("SELECT cs FROM CourseSeries cs JOIN cs.studyGroups sg JOIN sg.memberships m WHERE m.student.appUser.id = :studentId")
    List<CourseSeries> findByStudentId(@org.springframework.data.repository.query.Param("studentId") Long studentId);
}
