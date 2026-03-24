package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "institution_info")
public class InstitutionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String universityName;
    private String city;
    private String sekretariatEmail;
    private String sekretariatPhone;
    private String sekretariatOpeningTimes;
    private String websiteEmail;
    private String bibliothekUrl;
    private String mensaUrl;
    
    @Column(columnDefinition = "TEXT")
    private String impressum;
}
