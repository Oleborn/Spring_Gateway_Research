package oleborn.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(HttpTimeoutException.class)
    public ResponseEntity<?> timeout(HttpTimeoutException ex) {
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body("Upstream timeout");
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<?> resource(ResourceAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Upstream unavailable");
    }
}
