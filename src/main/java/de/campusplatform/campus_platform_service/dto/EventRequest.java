package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.EventType;
import java.time.LocalDateTime;

public record EventRequest(
        java.util.List<Long> roomIds,
        String name,
        EventType eventType,
        java.time.LocalDateTime startTime,
        Integer durationMinutes
) {}
