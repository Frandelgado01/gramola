package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage() != null ? ex.getMessage() : "Error";

        // Login-specific cases
        if (message.toLowerCase().contains("credenciales")) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (message.toLowerCase().contains("confirm")) {
            status = HttpStatus.FORBIDDEN;
        }

        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error interno del servidor"));
    }
}
