package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.EventType;
import java.time.LocalDateTime;

public record AdminEventResponse(
        Long id,
        Long courseSeriesId,
        Long roomId,
        String roomName,
        String name,
        EventType eventType,
        LocalDateTime startTime,
        Integer durationMinutes
) {}
