package de.campusplatform.campus_platform_service.dto;

public record RoomScheduleEventResponse(
        Long eventId,
        String eventName,
        String eventType,
        Long roomId,
        String roomName,
        String startTime,
        Integer durationMinutes,
        Long courseSeriesId,
        String moduleName) {
}
