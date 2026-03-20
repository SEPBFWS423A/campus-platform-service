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
    private Integer requiredTotalMinutes;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<ExamType> possibleExamTypes;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ModuleLecturer> qualifiedLecturers;

    @OneToMany(mappedBy = "module")
    private Set<CourseSeries> courseSeries;
}
