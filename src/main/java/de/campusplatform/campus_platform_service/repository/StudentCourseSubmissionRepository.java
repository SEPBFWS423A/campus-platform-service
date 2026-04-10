package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import de.campusplatform.campus_platform_service.model.StudentCourseSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

