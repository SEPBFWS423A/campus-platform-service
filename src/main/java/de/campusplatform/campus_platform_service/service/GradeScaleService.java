package de.campusplatform.campus_platform_service.service;

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
}
