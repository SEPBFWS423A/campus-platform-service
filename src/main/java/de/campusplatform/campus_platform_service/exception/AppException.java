package de.campusplatform.campus_platform_service.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final String messageKey;

    public AppException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
