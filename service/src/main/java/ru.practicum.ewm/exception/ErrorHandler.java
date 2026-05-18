package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler({ConflictException.class, DuplicatedDataException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final RuntimeException e) {
        log.error("Конфликт: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.CONFLICT.name())
                .reason("Для запрашиваемой операции условия не выполнены.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyList())
                .build();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(final NotFoundException e) {
        log.error("Объект не найден: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.NOT_FOUND.name())
                .reason("Требуемый объект не был найден.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyList())
                .build();
    }

    @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class,  MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final Exception e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Запрос составлен некорректно.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyList())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("Внутренняя ошибка сервера.", e);
        return ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .reason("Произошла ошибка.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyList())
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityException(final DataIntegrityViolationException e) {
        String rootMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();

        log.error("Конфликт данных БД: {}", rootMessage);

        return ApiError.builder()
                .status(HttpStatus.CONFLICT.name())
                .reason("Конфликт данных.")
                .message(rootMessage)
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyList())
                .build();
    }
}
