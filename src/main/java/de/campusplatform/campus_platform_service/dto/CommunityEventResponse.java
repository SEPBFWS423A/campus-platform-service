package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.StudentEventCategory;
import java.time.LocalDateTime;

import java.util.List;

public record CommunityEventResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        StudentEventCategory category,
        Long creatorId,
        String creatorName,
        Long roomId,
        String roomName,
        String customLocation,
        List<AttendeeInfo> attendees
) {
}
