package com.training.guesstheword.service;

public class DailyLimitExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DailyLimitExceededException(String message) {
        super(message);
    }
}
