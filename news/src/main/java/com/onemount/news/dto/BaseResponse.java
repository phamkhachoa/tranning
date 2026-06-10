package com.onemount.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(int code, String message, T data, List<String> errors) {

    private static final String SUCCESS_MESSAGE = "Success";

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, SUCCESS_MESSAGE, data, null);
    }

    public static <T> BaseResponse<T> success(int code, T data) {
        return new BaseResponse<>(code, SUCCESS_MESSAGE, data, null);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, message, null, null);
    }

    public static <T> BaseResponse<T> error(int code, String message, List<String> errors) {
        return new BaseResponse<>(code, message, null, errors);
    }
}
