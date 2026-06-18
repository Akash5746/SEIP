package com.seip.expense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidExpenseStateException extends RuntimeException {

    public InvalidExpenseStateException(String message) {
        super(message);
    }
}
