package com.onemount.news.exception;

public class BadRequestException extends RuntimeException {

    private final String message;

    public BadRequestException(String message, Object... args) {
        this.message = String.format(message, args);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
