package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "module")
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer semester;
    private Integer requiredTotalHours;

    @Column(name = "ects")
    private Integer ects;

    @ManyToMany
    @JoinTable(
        name = "module_exam_types",
        joinColumns = @JoinColumn(name = "module_id"),
        inverseJoinColumns = @JoinColumn(name = "exam_type_id")
    )
    private java.util.Set<ExamType> possibleExamTypes;

    @ManyToOne
    @JoinColumn(name = "preferred_exam_type_id")
    private ExamType preferredExamType;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ModuleLecturer> qualifiedLecturers;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_of_study_id")
    private CourseOfStudy courseOfStudy;

    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @OneToMany(mappedBy = "module")
    private Set<CourseSeries> courseSeries;
}
