package teccr.justdoitcloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); //404
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Void> handleForbidden(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); //403
    }

    @ExceptionHandler(TaskGenerationException.class)
    public ResponseEntity<String> handleGeneration(TaskGenerationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage());
    }
}