package com.flowforge.auth_service.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    // Basic structure
        public ResourceAlreadyExistsException(String message) {
            super(message);  // passes message to RuntimeException
        }

    }



