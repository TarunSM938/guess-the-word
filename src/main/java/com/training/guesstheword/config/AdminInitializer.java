package com.training.guesstheword.config;

import com.training.guesstheword.repository.UserRepository;
import com.training.guesstheword.service.UserValidator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            UserValidator userValidator,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        List<String> errors = new ArrayList<>();
        errors.addAll(userValidator.validateUsername(adminUsername));
        errors.addAll(userValidator.validatePassword(adminPassword));
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Configured admin credentials are invalid: " + String.join(" ", errors));
        }

        userRepository.save(adminUsername, passwordEncoder.encode(adminPassword), "ADMIN");
    }
}
