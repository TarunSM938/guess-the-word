package com.training.guesstheword.repository;

import com.training.guesstheword.model.GameSession;
import com.training.guesstheword.model.GuessRecord;
import com.training.guesstheword.model.Word;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class GameRepository {
    private static final String SESSION_COLUMNS = "id, user_id, word_id, status, played_on";

    private final JdbcTemplate jdbcTemplate;

    public GameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<GameSession> findInProgressToday(long userId, String date) {
        List<GameSession> sessions = jdbcTemplate.query(
                "SELECT " + SESSION_COLUMNS
                        + " FROM game_sessions WHERE user_id = ? AND played_on = ?"
                        + " AND status = ? ORDER BY id LIMIT 1",
                (resultSet, rowNumber) -> new GameSession(
                        resultSet.getLong("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getLong("word_id"),
                        resultSet.getString("status"),
                        resultSet.getString("played_on")),
                userId,
                date,
                "IN_PROGRESS");
        return sessions.stream().findFirst();
    }

    public int countSessionsOn(long userId, String date) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM game_sessions WHERE user_id = ? AND played_on = ?",
                Integer.class,
                userId,
                date);
        return count == null ? 0 : count;
    }

    public Optional<Word> pickRandomWordNotUsedOn(long userId, String date) {
        List<Word> words = jdbcTemplate.query(
                "SELECT id, word FROM words WHERE id NOT IN ("
                        + "SELECT word_id FROM game_sessions WHERE user_id = ? AND played_on = ?"
                        + ") ORDER BY RANDOM() LIMIT 1",
                (resultSet, rowNumber) -> new Word(
                        resultSet.getLong("id"),
                        resultSet.getString("word")),
                userId,
                date);
        return words.stream().findFirst();
    }

    public long createSession(long userId, long wordId, String date) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO game_sessions (user_id, word_id, status, played_on)"
                            + " VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setLong(2, wordId);
            statement.setString(3, "IN_PROGRESS");
            statement.setString(4, date);
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return the new game session ID.");
        }
        return generatedId.longValue();
    }

    public Optional<GameSession> findSessionById(long sessionId) {
        List<GameSession> sessions = jdbcTemplate.query(
                "SELECT " + SESSION_COLUMNS + " FROM game_sessions WHERE id = ?",
                (resultSet, rowNumber) -> new GameSession(
                        resultSet.getLong("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getLong("word_id"),
                        resultSet.getString("status"),
                        resultSet.getString("played_on")),
                sessionId);
        return sessions.stream().findFirst();
    }

    public Optional<String> findAnswer(long wordId) {
        List<String> answers = jdbcTemplate.query(
                "SELECT word FROM words WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getString("word"),
                wordId);
        return answers.stream().findFirst();
    }

    public List<GuessRecord> findGuesses(long sessionId) {
        return jdbcTemplate.query(
                "SELECT guess_word, guess_number, result_pattern FROM guesses"
                        + " WHERE session_id = ? ORDER BY guess_number",
                (resultSet, rowNumber) -> new GuessRecord(
                        resultSet.getString("guess_word"),
                        resultSet.getInt("guess_number"),
                        resultSet.getString("result_pattern")),
                sessionId);
    }

    public void saveGuess(long sessionId, String guessWord, int guessNumber, String resultPattern) {
        jdbcTemplate.update(
                "INSERT INTO guesses (session_id, guess_word, guess_number, result_pattern)"
                        + " VALUES (?, ?, ?, ?)",
                sessionId,
                guessWord,
                guessNumber,
                resultPattern);
    }

    public void updateStatus(long sessionId, String status) {
        jdbcTemplate.update(
                "UPDATE game_sessions SET status = ? WHERE id = ?",
                status,
                sessionId);
    }
}
