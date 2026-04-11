package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.EventType;
import java.time.LocalDateTime;

public record AdminEventResponse(
        Long id,
        Long courseSeriesId,
        java.util.List<RoomResponse> rooms,
        String name,
        EventType eventType,
        java.time.LocalDateTime startTime,
        Integer durationMinutes
) {
    public record RoomResponse(Long id, String name) {}
}
