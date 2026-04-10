package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.ExamDocumentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "exam_document")
public class ExamDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_series_id")
    private CourseSeries courseSeries;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private ExamDocumentType type;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content; // Base64 encoded or text content
}
