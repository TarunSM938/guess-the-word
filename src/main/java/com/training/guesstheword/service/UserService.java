package com.training.guesstheword.service;

import com.training.guesstheword.model.User;
import com.training.guesstheword.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String PLAYER_ROLE = "PLAYER";

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserValidator userValidator,
            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String username, String password) {
        List<String> errors = new ArrayList<>();
        errors.addAll(userValidator.validateUsername(username));
        errors.addAll(userValidator.validatePassword(password));

        if (username != null && userRepository.existsByUsername(username)) {
            errors.add("Username already exists.");
        }

        if (!errors.isEmpty()) {
            throw new UserValidationException(errors);
        }

        String passwordHash = passwordEncoder.encode(password);
        userRepository.save(username, passwordHash, PLAYER_ROLE);
    }

    public Optional<User> authenticate(String username, String password) {
        if (password == null) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
    }
}
