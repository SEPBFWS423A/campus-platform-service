package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.FaqResponse;
import de.campusplatform.campus_platform_service.dto.JobPostingResponse;
import de.campusplatform.campus_platform_service.enums.JobStatus;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import de.campusplatform.campus_platform_service.service.FaqService;
import de.campusplatform.campus_platform_service.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final InstitutionRepository institutionRepository;
    private final FaqService faqService;
    private final JobPostingService jobPostingService;

    @GetMapping("/university-name")
    public ResponseEntity<Map<String, String>> getUniversityName() {
        String name = institutionRepository.findAll().stream()
                .findFirst()
                .map(InstitutionInfo::getUniversityName)
                .orElse("CampusPlatform");

        return ResponseEntity.ok(Map.of("name", name));
    }

    @GetMapping("/faqs")
    public List<FaqResponse> getVisibleFaqs(@RequestParam(defaultValue = "de") String lang) {
        return faqService.getVisibleFaqs(lang);
    }

    @GetMapping("/job-postings")
    public List<JobPostingResponse> getActiveJobPostings() {
        return jobPostingService.getByStatus(JobStatus.AKTIV);
    }
}
