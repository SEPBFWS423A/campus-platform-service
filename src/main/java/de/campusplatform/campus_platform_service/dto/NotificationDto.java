package de.campusplatform.campus_platform_service.dto;

import java.time.LocalDateTime;

public record NotificationDto(
	Long id,
	String type,
	String icon,
	String colorClass,
	String text,
	String detail,
	boolean read,
	LocalDateTime createdAt
) {
}
