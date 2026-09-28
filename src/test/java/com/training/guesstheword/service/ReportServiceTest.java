package com.training.guesstheword.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.training.guesstheword.model.DayReport;
import com.training.guesstheword.model.GameState;
import com.training.guesstheword.model.UserDayRow;
import com.training.guesstheword.repository.GameRepository;
import com.training.guesstheword.repository.UserRepository;
import java.time.LocalDate;
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
class ReportServiceTest {
    private static final String PASSWORD = "ReportPass1$";

    @Autowired
    private ReportService reportService;

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
    void dayReportCountsOneUserAndOneWinForPastDate() {
        long userId = createUser();
        String reportDate = "2001-01-01";
        long wordId = firstWordId();
        long wonSession = gameRepository.createSession(userId, wordId, reportDate);
        gameRepository.updateStatus(wonSession, "WON");
        gameRepository.createSession(userId, nextWordId(wordId), reportDate);

        DayReport report = reportService.getDayReport(reportDate);

        assertEquals(reportDate, report.date());
        assertEquals(1, report.usersPlayed());
        assertEquals(1, report.correctGuesses());
    }

    @Test
    void emptyDayReportReturnsZeroCounts() {
        DayReport report = reportService.getDayReport("2000-01-01");

        assertEquals(0, report.usersPlayed());
        assertEquals(0, report.correctGuesses());
    }

    @Test
    void invalidDateIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getDayReport("not-a-date"));

        assertEquals("Enter a valid date", exception.getMessage());
    }

    @Test
    void unknownUsernameIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getUserReport("MissingReportUser"));

        assertEquals("No user with that username", exception.getMessage());
    }

    @Test
    void userReportCountsStartedGamesIncludingUnfinishedGames() {
        long userId = createUser();
        String today = LocalDate.now().toString();
        GameState firstGame = gameService.startOrResume(userId);
        gameService.submitGuess(userId, firstGame.sessionId(), answerFor(firstGame.sessionId()));
        gameService.startOrResume(userId);

        List<UserDayRow> report = reportService.getUserReport(
                userRepository.findByUsername(usernames.get(0)).orElseThrow().getUsername());

        assertEquals(1, report.size());
        assertEquals(today, report.get(0).date());
        assertEquals(2, report.get(0).wordsTried());
        assertEquals(1, report.get(0).correctGuesses());
    }

    private long createUser() {
        String randomId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder usernameBuilder = new StringBuilder("Report");
        for (char character : randomId.toCharArray()) {
            usernameBuilder.append((char) ('a' + Character.digit(character, 16)));
        }
        String username = usernameBuilder.toString();
        userService.register(username, PASSWORD);
        usernames.add(username);
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    private long firstWordId() {
        return jdbcTemplate.queryForObject("SELECT MIN(id) FROM words", Long.class);
    }

    private long nextWordId(long currentWordId) {
        return jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM words WHERE id > ?",
                Long.class,
                currentWordId);
    }

    private String answerFor(long sessionId) {
        long wordId = gameRepository.findSessionById(sessionId).orElseThrow().wordId();
        return gameRepository.findAnswer(wordId).orElseThrow();
    }
}
