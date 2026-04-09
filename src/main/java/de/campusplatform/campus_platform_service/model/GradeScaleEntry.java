package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grade_scale_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScaleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Double grade; // e.g., 1.0, 1.3

    @Column(nullable = false)
    private Double minimumPoints; // e.g., 95.0

    @Column
    private String label; // Optional label like "Excellent"
}
