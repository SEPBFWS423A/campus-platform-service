package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.AbsenceAuditAction;
import de.campusplatform.campus_platform_service.enums.AbsenceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "absence_audit_log")
public class AbsenceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long absenceId;

    @Enumerated(EnumType.STRING)
    private AbsenceAuditAction action;

    private String performedBy;

    private LocalDateTime performedAt;

    @Enumerated(EnumType.STRING)
    private AbsenceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private AbsenceStatus newStatus;

    @Column(length = 500)
    private String reason;
}
