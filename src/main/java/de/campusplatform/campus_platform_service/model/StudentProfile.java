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
@Table(name = "student_profile")
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    @Column(name = "student_number", unique = true, nullable = false)
    private String studentNumber;

    @Column(name = "start_year")
    private Integer startYear;

    @ManyToOne(optional = false)
    @JoinColumn(name = "focus_id")
    private Focus focus;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudyGroupMembership> memberships;
}
