package lk.ijse.edu.golankacourier.exception;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Simple runtime exception that carries an HTTP status and an optional code.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode; // optional machine-friendly code

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }

    public ApiException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = null;
    }
}
