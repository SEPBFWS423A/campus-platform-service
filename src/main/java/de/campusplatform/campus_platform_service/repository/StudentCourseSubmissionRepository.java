package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseSubmissionRepository extends JpaRepository<StudentCourseSubmission, Long> {
    List<StudentCourseSubmission> findByCourseSeriesId(Long courseSeriesId);
    Optional<StudentCourseSubmission> findByCourseSeriesIdAndStudentId(Long courseSeriesId, Long studentId);
    Long countByCourseSeriesIdAndStatus(Long courseSeriesId, SubmissionStatus status);

    @EntityGraph(attributePaths = {"documents", "courseSeries", "courseSeries.selectedExamType"})
    List<StudentCourseSubmission> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"documents", "courseSeries", "courseSeries.selectedExamType"})
    Optional<StudentCourseSubmission> findByIdAndStudentId(Long id, Long studentId);

    @org.springframework.data.jpa.repository.Query("SELECT cs FROM CourseSeries cs LEFT JOIN FETCH cs.studyGroups WHERE cs.id = :id")
    Optional<de.campusplatform.campus_platform_service.model.CourseSeries> findCourseSeriesWithGroupsById(Long id);

    @EntityGraph(attributePaths = {
            "courseSeries",
            "courseSeries.module",
            "courseSeries.selectedExamType",
            "courseSeries.events",
            "courseSeries.studyGroups"
    })
    @Query("""
    select distinct s
    from StudentCourseSubmission s
    where s.student.id = :studentId
      and s.courseSeries.id in :courseSeriesIds
""")
    List<StudentCourseSubmission> findOwnOverviewSubmissions(
            @Param("studentId") Long studentId,
            @Param("courseSeriesIds") Collection<Long> courseSeriesIds
    );
}

