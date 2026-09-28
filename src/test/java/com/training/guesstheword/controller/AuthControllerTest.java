package com.training.guesstheword.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.training.guesstheword.service.UserService;
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
class AuthControllerTest {
    private static final String TEST_USERNAME = "WebPlayer";
    private static final String TEST_PASSWORD = "Play*123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeExistingTestUser() {
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", TEST_USERNAME);
    }

    @AfterEach
    void removeTestUser() {
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", TEST_USERNAME);
    }

    @Test
    void registersValidPlayerAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", TEST_USERNAME)
                        .param("password", TEST_PASSWORD)
                        .param("confirmPassword", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void showsValidationErrorsAndNeverRedisplaysPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", TEST_USERNAME)
                        .param("password", "weak")
                        .param("confirmPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("Password must be at least 5 characters long.")))
                .andExpect(content().string(containsString("value=\"WebPlayer\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"weak\""))));
    }

    @Test
    void showsGenericMessageForIncorrectPassword() throws Exception {
        userService.register(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", TEST_USERNAME)
                        .param("password", "Wrong*123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    @Test
    void successfulPlayerLoginRedirectsToPlayerHome() throws Exception {
        userService.register(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", TEST_USERNAME)
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/player/home"));
    }

    @Test
    void loggedOutPlayerRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/player/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void playerCannotOpenAdminHome() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.USER_ID, 99L);
        session.setAttribute(SessionAttributes.USERNAME, TEST_USERNAME);
        session.setAttribute(SessionAttributes.ROLE, "PLAYER");

        mockMvc.perform(get("/admin/home").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
