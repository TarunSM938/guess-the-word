package com.training.guesstheword.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserValidatorTest {
    private final UserValidator validator = new UserValidator();

    @Test
    void acceptsValidUsername() {
        assertTrue(validator.validateUsername("Player").isEmpty());
    }

    @Test
    void rejectsUsernameThatIsTooShort() {
        assertFalse(validator.validateUsername("Abcd").isEmpty());
    }

    @Test
    void rejectsUsernameContainingDigit() {
        assertFalse(validator.validateUsername("Player1").isEmpty());
    }

    @Test
    void acceptsValidPassword() {
        assertTrue(validator.validatePassword("Alpha1$").isEmpty());
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertFalse(validator.validatePassword("Alpha$").isEmpty());
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        assertFalse(validator.validatePassword("12345$").isEmpty());
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() {
        assertFalse(validator.validatePassword("Alpha1").isEmpty());
    }

    @Test
    void rejectsPasswordThatIsTooShort() {
        assertFalse(validator.validatePassword("A1$").isEmpty());
    }
}
