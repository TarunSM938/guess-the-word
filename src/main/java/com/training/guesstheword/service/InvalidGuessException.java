package com.training.guesstheword.service;

public class InvalidGuessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidGuessException(String message) {
        super(message);
    }
}
