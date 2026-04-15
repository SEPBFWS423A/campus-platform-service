package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record FeedbackResponse(
    Long id,
    String content,
    LocalDateTime createdAt
) {}
