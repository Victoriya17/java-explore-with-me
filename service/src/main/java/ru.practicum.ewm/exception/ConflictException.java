package ru.practicum.ewm.exception;

import org.springframework.dao.DataIntegrityViolationException;

public class ConflictException extends RuntimeException {
    public ConflictException(DataIntegrityViolationException message) {
        super(message);
    }

    public ConflictException(String message) {
        super(message);
    }
}
