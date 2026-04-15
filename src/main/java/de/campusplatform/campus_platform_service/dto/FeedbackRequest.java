package de.campusplatform.campus_platform_service.dto;

public record FeedbackRequest(
    String content,
    Long lecturerId
) {}
