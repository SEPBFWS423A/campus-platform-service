package de.campusplatform.campus_platform_service.dto;

import java.util.List;

/**
 * HTTP 409-Response-Body bei Terminkonflikten beim Anlegen einer Abwesenheit.
 */
public record ConflictResponse(
    String message,
    List<ConflictingEventDto> conflictingEvents
) {}
