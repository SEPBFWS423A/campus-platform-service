package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AbsenceType;
import java.time.LocalDate;

public record LecturerAbsenceResponse(
    Long id,
    AbsenceType type,
    LocalDate startDate,
    LocalDate endDate,
    String note,
    String lecturerName // für Admin-Ansicht
) {}
