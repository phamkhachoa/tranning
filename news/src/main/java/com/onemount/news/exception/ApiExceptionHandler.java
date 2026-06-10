package com.onemount.news.exception;

import com.onemount.news.dto.BaseResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String ERROR_LOG_FORMAT = "Error: URI: {}, ErrorCode: {}, Message: {}";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFoundException(NotFoundException ex, WebRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = ex.getMessage();
        LOG.warn(ERROR_LOG_FORMAT, getServletPath(request), status.value(), message);
        return ResponseEntity.status(status).body(BaseResponse.error(status.value(), message));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();
        LOG.warn(ERROR_LOG_FORMAT, getServletPath(request), status.value(), message);
        return ResponseEntity.status(status).body(BaseResponse.error(status.value(), message));
    }

    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotImplementedException(
            NotImplementedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.NOT_IMPLEMENTED;
        String message = ex.getMessage();
        LOG.warn(ERROR_LOG_FORMAT, getServletPath(request), status.value(), message);
        return ResponseEntity.status(status).body(BaseResponse.error(status.value(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(HttpStatus.BAD_REQUEST.value(), "Request information is not valid", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleOtherException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        LOG.error(ERROR_LOG_FORMAT, getServletPath(request), status.value(), message, ex);
        return ResponseEntity.status(status).body(BaseResponse.error(status.value(), message));
    }

    private String getServletPath(WebRequest webRequest) {
        return webRequest.getDescription(false);
    }
}
