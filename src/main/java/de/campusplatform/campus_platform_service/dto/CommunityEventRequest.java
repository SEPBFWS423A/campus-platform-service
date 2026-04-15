package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.StudentEventCategory;
import java.time.LocalDateTime;

public record CommunityEventRequest(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        StudentEventCategory category,
        Long roomId,
        String customLocation
) {
}
