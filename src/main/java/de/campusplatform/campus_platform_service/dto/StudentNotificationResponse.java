package de.campusplatform.campus_platform_service.dto;

public record StudentNotificationResponse(
    String type,
    String icon,
    String colorClass,
    String text,
    String detail
) {}
