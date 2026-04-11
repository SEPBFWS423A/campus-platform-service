package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDate;
import java.util.List;

public record AutoScheduleRequest(
    LocalDate startDate,
    LocalDate endDate,
    List<TimeSlotDTO> timeSlots
) {}
