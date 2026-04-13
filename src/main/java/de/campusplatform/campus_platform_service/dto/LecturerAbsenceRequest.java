package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AbsenceType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LecturerAbsenceRequest(
    @NotNull AbsenceType type,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String note // optional
) {}
