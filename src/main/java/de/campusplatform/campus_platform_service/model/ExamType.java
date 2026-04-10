package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.ExamCategory;
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

    @Enumerated(EnumType.STRING)
    private ExamCategory category;

    private String nameDe;
    private String nameEn;
    private String shortDe;
    private String shortEn;
}
