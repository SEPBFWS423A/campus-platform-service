package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.BlockoutPriority;
import de.campusplatform.campus_platform_service.model.BlockoutReason;
import java.time.LocalDateTime;

public record RoomBlockoutResponse(
    Long id,
    Long roomId,
    String roomName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BlockoutReason reason,
    BlockoutPriority priority,
    String notes,
    String createdBy,
    LocalDateTime createdAt,
    boolean active
) {}
