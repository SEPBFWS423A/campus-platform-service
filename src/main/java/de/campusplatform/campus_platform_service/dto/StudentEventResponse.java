package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.EventType;
import java.time.LocalDateTime;
import java.util.List;

public record StudentEventResponse(
        Long id,
        String name,
        EventType eventType,
        LocalDateTime startTime,
        Integer durationMinutes,
        String moduleName,
        String lecturerName,
        List<String> rooms,
        boolean submission
) {
}
