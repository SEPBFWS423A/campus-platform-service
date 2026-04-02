package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_series")
public class CourseSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id")
    private Module module;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_lecturer_id")
    private AppUser assignedLecturer;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_exam_type_id")
    private ExamType selectedExamType;

    private String examFileUrl;
    private LocalDateTime submissionStartDate;
    private LocalDateTime submissionDeadline;

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Event> events;

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentCourseSubmission> studentSubmissions;

    @ManyToMany
    @JoinTable(
        name = "course_series_study_group",
        joinColumns = @JoinColumn(name = "course_series_id"),
        inverseJoinColumns = @JoinColumn(name = "study_group_id")
    )
    private Set<StudyGroup> studyGroups = new java.util.HashSet<>();
}
