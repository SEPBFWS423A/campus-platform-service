package de.campusplatform.campus_platform_service.exception;

import de.campusplatform.campus_platform_service.dto.ConflictingEventDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.CONFLICT)
public class LecturerAbsenceConflictException extends RuntimeException {

    private final List<ConflictingEventDto> conflictingEvents;

    public LecturerAbsenceConflictException(String message, List<ConflictingEventDto> conflictingEvents) {
        super(message);
        this.conflictingEvents = conflictingEvents;
    }

    public List<ConflictingEventDto> getConflictingEvents() {
        return conflictingEvents;
    }
}
