package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.model.GradeScaleEntry;
import de.campusplatform.campus_platform_service.service.GradeScaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/grade-scale")
@RequiredArgsConstructor
public class AdminGradeScaleController {

    private final GradeScaleService gradeScaleService;

    @GetMapping
    public List<GradeScaleEntry> getGradeScale() {
        return gradeScaleService.getAllEntries();
    }

    @PostMapping
    public GradeScaleEntry saveEntry(@RequestBody GradeScaleEntry entry) {
        return gradeScaleService.saveEntry(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        gradeScaleService.deleteEntry(id);
        return ResponseEntity.ok().build();
    }
}
