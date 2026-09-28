package com.training.guesstheword.service;

import java.util.List;

public class UserValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public UserValidationException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
