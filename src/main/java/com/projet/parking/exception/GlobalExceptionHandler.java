package com.projet.parking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceErrors(
            ResourceNotFoundException ex) {
        Map<String, String> errors = new HashMap<String, String>();
        errors.put(ex.getEntity(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(OperationNonAutoriseeException.class)
    public ResponseEntity<Map<String, String>> handleOperationNonAutorisee(
            OperationNonAutoriseeException ex) {
        Map<String, String> errors = new HashMap<String, String>();
        errors.put("erreur", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }
}