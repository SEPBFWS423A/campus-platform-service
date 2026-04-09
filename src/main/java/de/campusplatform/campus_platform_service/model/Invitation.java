package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.enums.InvitationStatus;
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
    
    private Long specializationId;
    private Integer startYear;
    private Integer startQuartal;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    public Invitation(String email, Role role, String studentNumber, Long specializationId, Integer startYear, Integer startQuartal) {
        this.email = email;
        this.role = role;
        this.studentNumber = studentNumber;
        this.specializationId = specializationId;
        this.startYear = startYear;
        this.startQuartal = startQuartal;
        this.token = UUID.randomUUID().toString();
        this.status = InvitationStatus.PENDING;
    }
}
