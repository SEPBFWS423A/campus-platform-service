package de.campusplatform.campus_platform_service;

import de.campusplatform.campus_platform_service.model.GradeScaleEntry;
import de.campusplatform.campus_platform_service.repository.GradeScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1) // Run before general data initializer
@RequiredArgsConstructor
public class GradeScaleInitializer implements CommandLineRunner {

    private final GradeScaleRepository gradeScaleRepository;

    @Override
    public void run(String... args) {
        if (gradeScaleRepository.count() == 0) {
            List<GradeScaleEntry> defaults = List.of(
                    GradeScaleEntry.builder().grade(1.0).minimumPoints(95.0).label("Sehr Gut").build(),
                    GradeScaleEntry.builder().grade(1.3).minimumPoints(90.0).label("Sehr Gut").build(),
                    GradeScaleEntry.builder().grade(1.7).minimumPoints(85.0).label("Gut").build(),
                    GradeScaleEntry.builder().grade(2.0).minimumPoints(80.0).label("Gut").build(),
                    GradeScaleEntry.builder().grade(2.3).minimumPoints(75.0).label("Gut").build(),
                    GradeScaleEntry.builder().grade(2.7).minimumPoints(70.0).label("Befriedigend").build(),
                    GradeScaleEntry.builder().grade(3.0).minimumPoints(65.0).label("Befriedigend").build(),
                    GradeScaleEntry.builder().grade(3.3).minimumPoints(60.0).label("Befriedigend").build(),
                    GradeScaleEntry.builder().grade(3.7).minimumPoints(55.0).label("Ausreichend").build(),
                    GradeScaleEntry.builder().grade(4.0).minimumPoints(50.0).label("Ausreichend").build(),
                    GradeScaleEntry.builder().grade(5.0).minimumPoints(0.0).label("Nicht Bestanden").build()
            );

            gradeScaleRepository.saveAll(defaults);
        }
    }
}
