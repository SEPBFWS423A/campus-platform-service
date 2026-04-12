package de.campusplatform.campus_platform_service.dto;

public record RoomUtilizationResponse(
    Long roomId,
    String roomName,
    int seats,
    int examSeats,
    double utilizationPercent,    // gebuchte Minuten / verfügbare Minuten * 100
    int bookedMinutes,
    int totalAvailableMinutes,
    long plannedEventCount,       // Events mit Startzeit in der Zukunft
    long pastEventCount           // Events mit Startzeit in der Vergangenheit
) {}
