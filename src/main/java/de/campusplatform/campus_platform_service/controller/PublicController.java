package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final InstitutionRepository institutionRepository;

    public PublicController(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @GetMapping("/university-name")
    public ResponseEntity<Map<String, String>> getUniversityName() {
        String name = institutionRepository.getFirst()
                .map(InstitutionInfo::getUniversityName)
                .orElse("CampusPlatform");
        return ResponseEntity.ok(Map.of("name", name));
    }
}
