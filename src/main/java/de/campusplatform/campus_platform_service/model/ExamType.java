package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exam_type")
public class ExamType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String type; // The internal code, e.g. KLAUSUR

    private boolean submission;

    private String nameDe;
    private String nameEn;
    private String shortDe;
    private String shortEn;
}
