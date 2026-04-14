package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record ConflictingEventDto(
    Long eventId,
    String eventName,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}
