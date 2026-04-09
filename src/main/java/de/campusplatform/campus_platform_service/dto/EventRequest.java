package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.EventType;
import java.time.LocalDateTime;

public record EventRequest(
        Long roomId,
        String name,
        EventType eventType,
        LocalDateTime startTime,
        Integer durationMinutes
) {}
