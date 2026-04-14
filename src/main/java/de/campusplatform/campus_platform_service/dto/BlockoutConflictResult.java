package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record BlockoutConflictResult(
    List<RoomBlockoutResponse> overlappingBlockouts,
    List<RoomScheduleEventResponse> affectedEvents
) {}
