package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final InstitutionRepository institutionRepository;

    public PublicController(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @GetMapping("/institution")
    public ResponseEntity<InstitutionInfo> getInstitutionInfo() {
        return institutionRepository.getFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new InstitutionInfo()));
    }
}
