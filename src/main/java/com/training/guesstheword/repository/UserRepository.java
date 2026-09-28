package com.training.guesstheword.repository;

import com.training.guesstheword.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNumber) ->
            new User(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("role"));

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, password, role FROM users WHERE username = ?",
                USER_ROW_MAPPER,
                username);
        return users.stream().findFirst();
    }

    public void save(String username, String passwordHash, String role) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                username,
                passwordHash,
                role);
    }

    public boolean existsByUsername(String username) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)",
                Boolean.class,
                username);
        return Boolean.TRUE.equals(exists);
    }
}
