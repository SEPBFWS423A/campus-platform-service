package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lastname;
    private String firstname;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled;

    private String theme;
    private String brightness;
}
