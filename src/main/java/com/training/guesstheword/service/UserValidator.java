package com.training.guesstheword.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {
    private static final String ALLOWED_SPECIAL_CHARACTERS = "$%*";

    public List<String> validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        if (username == null || username.length() < 5) {
            errors.add("Username must be at least 5 characters long.");
        }
        if (username == null || !username.matches("[A-Za-z]+")) {
            errors.add("Username may contain only letters A-Z and a-z.");
        }
        return errors;
    }

    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < 5) {
            errors.add("Password must be at least 5 characters long.");
        }
        if (password == null || !password.matches(".*[A-Za-z].*")) {
            errors.add("Password must contain at least one letter.");
        }
        if (password == null || !password.matches(".*[0-9].*")) {
            errors.add("Password must contain at least one digit.");
        }
        if (password == null || password.chars().noneMatch(
                character -> ALLOWED_SPECIAL_CHARACTERS.indexOf(character) >= 0)) {
            errors.add("Password must contain at least one of these special characters: $ % *.");
        }
        return errors;
    }
}
