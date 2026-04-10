package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "student_course_submission")
public class StudentCourseSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private AppUser student;

    @ManyToOne
    @JoinColumn(name = "course_series_id")
    private CourseSeries courseSeries;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    private String documentUrl;
    private LocalDateTime submissionDate;
    private Double grade;
    private Double points;
    private String feedback;
}
