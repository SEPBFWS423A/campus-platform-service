package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.JobStatus;
import de.campusplatform.campus_platform_service.enums.JobType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "job_posting")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.ENTWURF;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private LocalDate deadline;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Anzahl Bewerbungen – wird per Service hochgezählt */
    @Builder.Default
    private Integer applicationCount = 0;

    /** Automatisch veröffentlichen beim Anlegen */
    @Builder.Default
    private Boolean autoPublish = true;

    /** Admin der die Ausschreibung erstellt hat */
    private String createdBy;
}
