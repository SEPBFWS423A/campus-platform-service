package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.enums.Salutation;
import de.campusplatform.campus_platform_service.enums.AcademicTitle;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Salutation salutation;

    @Enumerated(EnumType.STRING)
    private AcademicTitle title;
    private String lastName;
    private String firstName;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled;

    private String theme;
    private String brightness;
    private String language;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "start_quartal")
    private Integer startQuartal;

    @OneToOne(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private StudentProfile studentProfile;

    @OneToMany(mappedBy = "lecturer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ModuleLecturer> teachingQualifications;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentCourseSubmission> courseSubmissions;

    @OneToMany(mappedBy = "lecturer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LecturerAbsence> absences;
}
