package com.training.guesstheword.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.training.guesstheword.model.GameState;
import com.training.guesstheword.model.GuessRecord;
import com.training.guesstheword.repository.GameRepository;
import com.training.guesstheword.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class GameServiceTest {
    private static final String PASSWORD = "GamePass1$";

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> usernames = new ArrayList<>();

    @BeforeEach
    void clearTrackedUsers() {
        usernames.clear();
    }

    @AfterEach
    void removeTestUsersAndGames() {
        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(user -> {
                jdbcTemplate.update(
                        "DELETE FROM guesses WHERE session_id IN"
                                + " (SELECT id FROM game_sessions WHERE user_id = ?)",
                        user.getId());
                jdbcTemplate.update("DELETE FROM game_sessions WHERE user_id = ?", user.getId());
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());
            });
        }
    }

    @Test
    void startingGameCreatesInProgressSessionWithFiveGuesses() {
        long userId = createUser();

        GameState state = gameService.startOrResume(userId);

        assertEquals("IN_PROGRESS", state.status());
        assertEquals(5, state.guessesRemaining());
        assertTrue(state.guesses().isEmpty());
        assertNull(state.answer());
    }

    @Test
    void startingAgainResumesSameSession() {
        long userId = createUser();

        GameState first = gameService.startOrResume(userId);
        gameService.submitGuess(userId, first.sessionId(), "ZZZZZ");
        GameState resumed = gameService.startOrResume(userId);

        assertEquals(first.sessionId(), resumed.sessionId());
        assertEquals(4, resumed.guessesRemaining());
        assertEquals(2, gameService.wordsRemainingToday(userId));
    }

    @Test
    void correctGuessWinsAndRevealsAnswer() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);
        String answer = answerFor(started.sessionId());

        GameState result = gameService.submitGuess(userId, started.sessionId(), answer);

        assertEquals("WON", result.status());
        assertEquals(answer, result.answer());
    }

    @Test
    void wrongGuessKeepsGameInProgressAndUsesOneGuess() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);

        GameState result = gameService.submitGuess(userId, started.sessionId(), "ZZZZZ");

        assertEquals("IN_PROGRESS", result.status());
        assertEquals(4, result.guessesRemaining());
        assertNull(result.answer());
    }

    @Test
    void fifthWrongGuessLosesAndRevealsAnswer() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);
        String answer = answerFor(started.sessionId());

        GameState result = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            String wrongGuess = answer.charAt(0) == 'Z' ? "AAAAA" : "ZZZZZ";
            result = gameService.submitGuess(userId, started.sessionId(), wrongGuess);
        }

        assertEquals("LOST", result.status());
        assertEquals(answer, result.answer());
        assertEquals(0, result.guessesRemaining());
    }

    @Test
    void rejectsGuessAfterGameEnds() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);
        gameService.submitGuess(userId, started.sessionId(), answerFor(started.sessionId()));

        InvalidGuessException exception = assertThrows(
                InvalidGuessException.class,
                () -> gameService.submitGuess(userId, started.sessionId(), "WRONG"));

        assertEquals("This game is already finished", exception.getMessage());
    }

    @Test
    void invalidLengthGuessDoesNotSaveAnything() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);

        assertThrows(
                InvalidGuessException.class,
                () -> gameService.submitGuess(userId, started.sessionId(), "FOUR"));
        assertThrows(
                InvalidGuessException.class,
                () -> gameService.submitGuess(userId, started.sessionId(), "AB12!"));

        assertTrue(gameRepository.findGuesses(started.sessionId()).isEmpty());
    }

    @Test
    void acceptsLowercaseGuessAndStoresItUppercase() {
        long userId = createUser();
        GameState started = gameService.startOrResume(userId);

        GameState result = gameService.submitGuess(userId, started.sessionId(), "  crane  ");

        GuessRecord savedGuess = gameRepository.findGuesses(started.sessionId()).get(0);
        assertEquals("CRANE", savedGuess.guessWord());
        assertEquals(1, result.guesses().get(0).guessNumber());
    }

    @Test
    void limitsPlayerToThreeFinishedGamesPerDay() {
        long userId = createUser();

        for (int gameNumber = 0; gameNumber < 3; gameNumber++) {
            GameState game = gameService.startOrResume(userId);
            String answer = answerFor(game.sessionId());
            gameService.submitGuess(userId, game.sessionId(), answer);
        }

        assertEquals(0, gameService.wordsRemainingToday(userId));
        assertThrows(DailyLimitExceededException.class, () -> gameService.startOrResume(userId));
    }

    @Test
    void differentUserCannotSubmitGuessToAnotherUsersSession() {
        long ownerId = createUser();
        long otherUserId = createUser();
        GameState game = gameService.startOrResume(ownerId);

        assertThrows(
                IllegalArgumentException.class,
                () -> gameService.submitGuess(otherUserId, game.sessionId(), "CRANE"));
    }

    private long createUser() {
        String randomId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder usernameBuilder = new StringBuilder("Game");
        for (char character : randomId.toCharArray()) {
            usernameBuilder.append((char) ('a' + Character.digit(character, 16)));
        }
        String username = usernameBuilder.toString();
        userService.register(username, PASSWORD);
        usernames.add(username);
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    private String answerFor(long sessionId) {
        long wordId = gameRepository.findSessionById(sessionId).orElseThrow().wordId();
        return gameRepository.findAnswer(wordId).orElseThrow();
    }
}
