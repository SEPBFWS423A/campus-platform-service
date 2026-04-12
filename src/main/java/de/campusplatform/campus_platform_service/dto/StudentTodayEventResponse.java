package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record StudentTodayEventResponse(
    Long eventId,
    String eventName,
    String eventType,
    String moduleName,
    String roomName,
    LocalDateTime startTime,
    Integer durationMinutes
) {}
