package com.training.guesstheword.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.training.guesstheword.model.User;
import com.training.guesstheword.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class UserServiceTest {
    private static final String USERNAME = "TestPlayer";
    private static final String PASSWORD = "Player1$";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestUser() {
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", USERNAME);
    }

    @Test
    void registersAndAuthenticatesUserWithHashedPassword() {
        userService.register(USERNAME, PASSWORD);

        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertEquals("PLAYER", user.getRole());
        assertTrue(userService.authenticate(USERNAME, PASSWORD).isPresent());
        assertFalse(userService.authenticate(USERNAME, "Wrong1$").isPresent());
    }

    @Test
    void rejectsDuplicateUsername() {
        userService.register(USERNAME, PASSWORD);

        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userService.register(USERNAME, PASSWORD));

        assertTrue(exception.getErrors().contains("Username already exists."));
        assertTrue(userRepository.existsByUsername(USERNAME));
    }

    @Test
    void doesNotSaveInvalidRegistration() {
        assertThrows(
                UserValidationException.class,
                () -> userService.register(USERNAME, "weak"));

        assertFalse(userRepository.existsByUsername(USERNAME));
    }
}
