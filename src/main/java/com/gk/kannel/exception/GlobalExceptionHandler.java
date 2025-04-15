package com.gk.kannel.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handle(InvalidRequestException ex){
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getKey(), ex.getMessage());

        response.put("errors",errors);
        return response;
    }
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handle(AuthenticationException ex){
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getKey(), ex.getMessage());

        response.put("errors",errors);
        return response;
    }
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handle(EntityNotFoundException ex){
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getKey(), ex.getMessage());

        response.put("errors",errors);
        return response;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        response.put("errors",errors);
        return response;
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();
            if (ife.getTargetType().isEnum()) {
                String errorMessage = "Invalid value for enum " + ife.getTargetType().getSimpleName() + ": " + ife.getValue() + ". Allowed values are: " + Arrays.stream(ife.getTargetType().getEnumConstants()).map(Object::toString).collect(Collectors.joining(","));
                errors.put("errorMsg", errorMessage);
            }
        }
        response.put("errors",errors);
        return response;
    }
}
