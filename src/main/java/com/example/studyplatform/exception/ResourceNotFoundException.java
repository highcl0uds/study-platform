package com.example.studyplatform.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " с id=" + id + " не найден(а)");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
