package com.flowforge.execution_service.exception;

import com.flowforge.execution_service.model.dto.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public SuccessResponse handleAlreadyExists(ResourceAlreadyExistsException ex) {
        return SuccessResponse.builder().success(false).message(ex.getMessage()).build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SuccessResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return SuccessResponse.builder().success(false).message(ex.getMessage()).build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public SuccessResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return SuccessResponse.builder().success(false).message(ex.getMessage()).build();
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public SuccessResponse handleRateLimit(RateLimitExceededException ex) {
        return SuccessResponse.builder().success(false).message(ex.getMessage()).build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public SuccessResponse handleIllegalState(IllegalStateException ex) {
        return SuccessResponse.builder().success(false).message(ex.getMessage()).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public SuccessResponse handleGenericException(Exception ex) {
        return SuccessResponse.builder().success(false).message("Something went wrong. Please try again.").build();
    }
}
