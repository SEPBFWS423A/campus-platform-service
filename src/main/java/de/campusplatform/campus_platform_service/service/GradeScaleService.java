package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.GradeScaleEntryResponse;
import de.campusplatform.campus_platform_service.dto.GradeScaleResponse;
import de.campusplatform.campus_platform_service.model.GradeScaleEntry;
import de.campusplatform.campus_platform_service.repository.GradeScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeScaleService {

    private final GradeScaleRepository gradeScaleRepository;

    public List<GradeScaleEntry> getAllEntries() {
        return gradeScaleRepository.findAllByOrderByMinimumPointsDesc();
    }

    public GradeScaleResponse getGradeScaleResponse() {
        return GradeScaleResponse.builder()
                .entries(
                        getAllEntries().stream()
                                .map(this::toResponse)
                                .toList()
                )
                .build();
    }

    @Transactional
    public GradeScaleEntry saveEntry(GradeScaleEntry entry) {
        return gradeScaleRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        gradeScaleRepository.deleteById(id);
    }

    public Double calculateGrade(Double points) {
        if (points == null) return null;

        List<GradeScaleEntry> entries = getAllEntries();
        for (GradeScaleEntry entry : entries) {
            if (points >= entry.getMinimumPoints()) {
                return entry.getGrade();
            }
        }

        // If no entry found (below lowest threshold), it's usually 5.0
        return 5.0;
    }

    private GradeScaleEntryResponse toResponse(GradeScaleEntry entry) {
        return GradeScaleEntryResponse.builder()
                .grade(entry.getGrade())
                .minimumPoints(entry.getMinimumPoints())
                .label(entry.getLabel())
                .build();
    }
}
