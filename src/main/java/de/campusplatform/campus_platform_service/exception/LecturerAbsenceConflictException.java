package de.campusplatform.campus_platform_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LecturerAbsenceConflictException extends RuntimeException {
    public LecturerAbsenceConflictException(String message) {
        super(message);
    }
}
