package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "public_profile")
public class PublicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(length = 1000)
    private String bio;

    private String interests;
    private String hobbies;
    private String skills;

    @Builder.Default
    private boolean visibility = true;
}
