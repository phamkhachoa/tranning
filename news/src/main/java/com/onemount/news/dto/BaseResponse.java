package com.onemount.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    private static final String SUCCESS_MESSAGE = "Success";

    private int code;
    private String message;
    private T data;
    private List<String> errors;

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
