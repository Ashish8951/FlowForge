package com.flowforge.workflow_service.exception;

import com.flowforge.workflow_service.model.dto.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice  // intercepts ALL exceptions in the app
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)  // 409
    public SuccessResponse handleAlreadyExists(ResourceAlreadyExistsException ex) {
        return SuccessResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SuccessResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return SuccessResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)  // 401
    public SuccessResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return SuccessResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build();
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
    public SuccessResponse handleGenericException(Exception ex) {
        return SuccessResponse.builder()
                .success(false)
                .message("Something went wrong. Please try again.")
                .build();
    }
}
