package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.JobPostingRequest;
import de.campusplatform.campus_platform_service.dto.JobPostingResponse;
import de.campusplatform.campus_platform_service.enums.JobStatus;
import de.campusplatform.campus_platform_service.model.JobPosting;
import de.campusplatform.campus_platform_service.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository repository;

    // ── Lesen ────────────────────────────────────────────────────────────────

    public List<JobPostingResponse> getAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<JobPostingResponse> getByStatus(JobStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toResponse).toList();
    }

    public JobPostingResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Schreiben ─────────────────────────────────────────────────────────────

    public JobPostingResponse create(JobPostingRequest req, String createdBy) {
        JobStatus initialStatus = Boolean.TRUE.equals(req.autoPublish())
                ? JobStatus.AKTIV : JobStatus.ENTWURF;

        JobPosting posting = JobPosting.builder()
                .title(req.title())
                .department(req.department())
                .type(req.type())
                .status(initialStatus)
                .description(req.description())
                .requirements(req.requirements())
                .deadline(req.deadline())
                .autoPublish(req.autoPublish() != null ? req.autoPublish() : true)
                .createdBy(createdBy)
                .build();

        return toResponse(repository.save(posting));
    }

    public JobPostingResponse update(Long id, JobPostingRequest req) {
        JobPosting posting = findOrThrow(id);
        posting.setTitle(req.title());
        posting.setDepartment(req.department());
        posting.setType(req.type());
        posting.setDescription(req.description());
        posting.setRequirements(req.requirements());
        posting.setDeadline(req.deadline());
        if (req.autoPublish() != null) {
            posting.setAutoPublish(req.autoPublish());
            if (Boolean.TRUE.equals(req.autoPublish())) {
                // Toggle ON → Posting ist öffentlich sichtbar
                posting.setStatus(JobStatus.AKTIV);
            } else if (posting.getStatus() == JobStatus.AKTIV) {
                // Toggle OFF bei aktiver Ausschreibung → vom öffentlichen Board entfernen
                posting.setStatus(JobStatus.GESCHLOSSEN);
            }
        }
        return toResponse(repository.save(posting));
    }

    public JobPostingResponse setStatus(Long id, JobStatus newStatus) {
        JobPosting posting = findOrThrow(id);
        posting.setStatus(newStatus);
        return toResponse(repository.save(posting));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stellenausschreibung nicht gefunden");
        }
        repository.deleteById(id);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private JobPosting findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Stellenausschreibung nicht gefunden"));
    }

    private JobPostingResponse toResponse(JobPosting p) {
        return new JobPostingResponse(
                p.getId(), p.getTitle(), p.getDepartment(), p.getType(), p.getStatus(),
                p.getDescription(), p.getRequirements(), p.getDeadline(),
                p.getCreatedAt(), p.getApplicationCount(), p.getAutoPublish(), p.getCreatedBy()
        );
    }
}
