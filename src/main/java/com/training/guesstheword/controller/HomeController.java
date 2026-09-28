package com.training.guesstheword.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/player/home")
    public String playerHome(HttpSession session, Model model) {
        if (!hasRole(session, "PLAYER")) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute(SessionAttributes.USERNAME));
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
