package com.training.guesstheword.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loggedOutVisitorIsRedirectedFromDayReport() throws Exception {
        mockMvc.perform(get("/admin/reports/day"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void playerIsRedirectedFromUserReport() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.USER_ID, 55L);
        session.setAttribute(SessionAttributes.USERNAME, "ReportPlayer");
        session.setAttribute(SessionAttributes.ROLE, "PLAYER");

        mockMvc.perform(get("/admin/reports/user").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanOpenDayReport() throws Exception {
        mockMvc.perform(get("/admin/reports/day").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("report-day"))
                .andExpect(content().string(containsString("Users who played")))
                .andExpect(content().string(containsString("Correct guesses")));
    }

    @Test
    void unknownUsernameShowsReportError() throws Exception {
        mockMvc.perform(get("/admin/reports/user")
                        .session(adminSession())
                        .param("username", "NoSuchReportUser"))
                .andExpect(status().isOk())
                .andExpect(view().name("report-user"))
                .andExpect(content().string(containsString("No user with that username")));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.USER_ID, 99L);
        session.setAttribute(SessionAttributes.USERNAME, "Administrator");
        session.setAttribute(SessionAttributes.ROLE, "ADMIN");
        return session;
    }
}
