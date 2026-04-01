package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String studentNumber;
    private String token;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    public Invitation(String email, Role role, String studentNumber) {
        this.email = email;
        this.role = role;
        this.studentNumber = studentNumber;
        this.token = UUID.randomUUID().toString();
        this.status = InvitationStatus.PENDING;
    }
}
