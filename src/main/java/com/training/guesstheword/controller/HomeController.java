package com.training.guesstheword.controller;

import com.training.guesstheword.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final GameService gameService;

    public HomeController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/player/home")
    public String playerHome(HttpSession session, Model model) {
        if (!hasRole(session, "PLAYER")) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute(SessionAttributes.USERNAME));
        model.addAttribute(
                "wordsRemaining",
                gameService.wordsRemainingToday((long) session.getAttribute(SessionAttributes.USER_ID)));
        return "player-home";
    }

    @GetMapping("/admin/home")
    public String adminHome(HttpSession session, Model model) {
        if (!hasRole(session, "ADMIN")) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute(SessionAttributes.USERNAME));
        return "admin-home";
    }

    private boolean hasRole(HttpSession session, String requiredRole) {
        return session.getAttribute(SessionAttributes.USERNAME) != null
                && requiredRole.equals(session.getAttribute(SessionAttributes.ROLE));
    }
}
