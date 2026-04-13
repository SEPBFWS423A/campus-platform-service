package de.campusplatform.campus_platform_service.model;

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
}
