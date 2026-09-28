package com.training.guesstheword.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.training.guesstheword.model.GameState;
import com.training.guesstheword.repository.GameRepository;
import com.training.guesstheword.repository.UserRepository;
import com.training.guesstheword.service.GameService;
import com.training.guesstheword.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {
    private static final String PASSWORD = "GamePage1$";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

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
    void loggedOutGamePageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/player/game/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void playerCanStartGame() throws Exception {
        long userId = createUser();

        mockMvc.perform(post("/player/game/start").session(playerSession(userId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrlPattern("/player/game/*"));
    }

    @Test
    void invalidGuessRedirectsBackWithFlashError() throws Exception {
        long userId = createUser();
        GameState game = gameService.startOrResume(userId);

        mockMvc.perform(post("/player/game/{sessionId}/guess", game.sessionId())
                        .session(playerSession(userId))
                        .param("guess", "FOUR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/player/game/" + game.sessionId()))
                .andExpect(flash().attribute("error", containsString("5 letters A-Z")));

        mockMvc.perform(get("/player/game/{sessionId}", game.sessionId())
                        .session(playerSession(userId))
                        .flashAttr("error", "Guess must be exactly 5 letters A-Z."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Guess must be exactly 5 letters A-Z.")));
    }

    @Test
    void newGamePageDoesNotRevealAnswer() throws Exception {
        long userId = createUser();
        GameState game = gameService.startOrResume(userId);
        String answer = answerFor(game.sessionId());

        mockMvc.perform(get("/player/game/{sessionId}", game.sessionId())
                        .session(playerSession(userId)))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andExpect(content().string(not(containsString(answer))));
    }

    @Test
    void pageRendersGreenOrangeAndGreyTilesFromResultPattern() throws Exception {
        long userId = createUser();
        GameState game = gameService.startOrResume(userId);
        String answer = answerFor(game.sessionId());
        String guess = guessWithMixedResults(answer);
        gameService.submitGuess(userId, game.sessionId(), guess);

        mockMvc.perform(get("/player/game/{sessionId}", game.sessionId())
                        .session(playerSession(userId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tile-green")))
                .andExpect(content().string(containsString("tile-orange")))
                .andExpect(content().string(containsString("tile-grey")));
    }

    @Test
    void playerCannotOpenAnotherPlayersGame() throws Exception {
        long ownerId = createUser();
        long otherUserId = createUser();
        GameState game = gameService.startOrResume(ownerId);

        mockMvc.perform(get("/player/game/{sessionId}", game.sessionId())
                        .session(playerSession(otherUserId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/player/home"));
    }

    private long createUser() {
        String randomId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder usernameBuilder = new StringBuilder("WebGame");
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

    private String guessWithMixedResults(String answer) {
        char[] guess = answer.toCharArray();
        int firstPosition = -1;
        int secondPosition = -1;
        for (int first = 0; first < 4 && firstPosition < 0; first++) {
            for (int second = first + 1; second < 4; second++) {
                if (answer.charAt(first) != answer.charAt(second)) {
                    firstPosition = first;
                    secondPosition = second;
                    break;
                }
            }
        }

        if (firstPosition < 0) {
            throw new IllegalStateException("Test word does not have distinct letters to swap.");
        }
        char swappedLetter = guess[firstPosition];
        guess[firstPosition] = guess[secondPosition];
        guess[secondPosition] = swappedLetter;

        for (char letter = 'A'; letter <= 'Z'; letter++) {
            if (answer.indexOf(letter) < 0) {
                guess[4] = letter;
                return new String(guess);
            }
        }
        throw new IllegalStateException("Test word has no absent letter.");
    }

    private MockHttpSession playerSession(long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.USER_ID, userId);
        session.setAttribute(SessionAttributes.USERNAME, "GamePlayer");
        session.setAttribute(SessionAttributes.ROLE, "PLAYER");
        return session;
    }
}
