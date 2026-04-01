package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final InstitutionRepository institutionRepository;

    @GetMapping("/institution")
    public ResponseEntity<InstitutionInfo> getInstitutionInfo() {
        return institutionRepository.getFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new InstitutionInfo()));
    }
}
