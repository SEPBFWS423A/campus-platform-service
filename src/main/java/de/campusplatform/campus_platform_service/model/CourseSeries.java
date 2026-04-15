package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.ExamStatus;
import de.campusplatform.campus_platform_service.enums.CourseStatus;
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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExamStatus examStatus = ExamStatus.OPEN;

    private String lecturerNotes;
    private LocalDateTime submissionStartDate;
    private LocalDateTime submissionDeadline;

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ExamDocument> documents = new java.util.HashSet<>();

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseDocument> courseDocuments = new java.util.HashSet<>();

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Event> events = new java.util.HashSet<>();

    @OneToMany(mappedBy = "courseSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentCourseSubmission> studentSubmissions = new java.util.HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "course_series_study_group",
        joinColumns = @JoinColumn(name = "course_series_id"),
        inverseJoinColumns = @JoinColumn(name = "study_group_id")
    )
    @Builder.Default
    private Set<StudyGroup> studyGroups = new java.util.HashSet<>();
}
