package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDate;

public record HolidayRequest(
    String name,
    LocalDate startDate,
    LocalDate endDate,
    String type
) {}