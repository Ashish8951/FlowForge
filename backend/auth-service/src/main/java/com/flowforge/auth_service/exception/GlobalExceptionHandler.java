package com.flowforge.auth_service.exception;

import com.flowforge.auth_service.model.dto.SuccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)  // 409
    public SuccessResponse handleAlreadyExists(ResourceAlreadyExistsException ex) {
        return SuccessResponse.builder()
                .status(false)
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SuccessResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return SuccessResponse.builder()
                .status(false)
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)  // 401
    public SuccessResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return SuccessResponse.builder()
                .status(false)
                .message(ex.getMessage())
                .build();
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public SuccessResponse handleGenericException(Exception ex) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return SuccessResponse.builder()
                .status(false)
                .message("Something went wrong. Please try again.")
                .build();
    }
}
