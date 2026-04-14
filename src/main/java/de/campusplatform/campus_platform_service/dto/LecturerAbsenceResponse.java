package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.AbsencePriority;
import de.campusplatform.campus_platform_service.enums.AbsenceStatus;
import de.campusplatform.campus_platform_service.enums.AbsenceType;
import java.time.LocalDate;

public record LecturerAbsenceResponse(
    Long id,
    AbsenceType type,
    LocalDate startDate,
    LocalDate endDate,
    String note,
    String lecturerName,
    // --- Governance-Felder (Issue #10) ---
    AbsenceStatus status,
    AbsencePriority priority,
    Boolean documentRequired,
    String approvedBy,
    String rejectionReason
) {}
