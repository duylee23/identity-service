package com.duyle.identity_service.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception!", HttpStatus.INTERNAL_SERVER_ERROR),
    KEY_INVALID(1000, "Key invalid!", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed!", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters!", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least {min} characters!", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "User not found!", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated!", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have access permission!", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Invalid date of birth, user must be at least {min}!", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;
}
