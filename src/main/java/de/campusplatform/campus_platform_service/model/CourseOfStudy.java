package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.DegreeType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_of_study", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "degree_type"}))
public class CourseOfStudy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DegreeType degreeType;

    @Column(name = "total_ects")
    private Integer totalEcts;

    @OneToMany(mappedBy = "courseOfStudy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Specialization> specializations;

    @OneToMany(mappedBy = "courseOfStudy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Module> modules;
}
