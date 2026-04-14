package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.AbsencePriority;
import de.campusplatform.campus_platform_service.enums.AbsenceStatus;
import de.campusplatform.campus_platform_service.enums.AbsenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "lecturer_absence")
public class LecturerAbsence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AbsenceType type;

    private String note;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    private AppUser lecturer;

    // --- Governance-Felder (Issue #10) ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AbsenceStatus status = AbsenceStatus.BEANTRAGT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AbsencePriority priority = AbsencePriority.MEDIUM;

    /** Vorlaufzeit in Tagen (automatisch beim Anlegen berechnet) */
    private Integer noticeDays;

    /** Dokumentationspflicht – true wenn Nachweis erforderlich */
    @Builder.Default
    private Boolean documentRequired = false;

    /** Username (E-Mail) des Admins, der genehmigt/abgelehnt hat */
    private String approvedBy;
    private LocalDateTime approvedAt;

    /** Begründung bei Ablehnung */
    private String rejectionReason;
}

