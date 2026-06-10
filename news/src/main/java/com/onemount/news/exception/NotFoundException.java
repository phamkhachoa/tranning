package com.onemount.news.exception;

public class NotFoundException extends RuntimeException {

    private final String message;

    public NotFoundException(String message, Object... args) {
        this.message = String.format(message, args);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
