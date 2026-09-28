package com.training.guesstheword.controller;

import com.training.guesstheword.model.GameState;
import com.training.guesstheword.service.DailyLimitExceededException;
import com.training.guesstheword.service.GameService;
import com.training.guesstheword.service.InvalidGuessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/player/game/start")
    public String start(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isPlayer(session)) {
            return "redirect:/login";
        }

        try {
            GameState game = gameService.startOrResume(userId(session));
            return "redirect:/player/game/" + game.sessionId();
        } catch (DailyLimitExceededException exception) {
            redirectAttributes.addFlashAttribute(
                    "message",
                    "You have used all 3 words for today. Come back tomorrow.");
            return "redirect:/player/home";
        }
    }

    @GetMapping("/player/game/{sessionId}")
    public String game(
            @PathVariable long sessionId,
            HttpSession session,
            Model model) {
        if (!isPlayer(session)) {
            return "redirect:/login";
        }

        try {
            GameState game = gameService.getState(userId(session), sessionId);
            model.addAttribute("gameState", game);
            model.addAttribute("username", session.getAttribute(SessionAttributes.USERNAME));
            return "game";
        } catch (IllegalArgumentException exception) {
            return "redirect:/player/home";
        }
    }

    @PostMapping("/player/game/{sessionId}/guess")
    public String guess(
            @PathVariable long sessionId,
            @RequestParam(required = false) String guess,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isPlayer(session)) {
            return "redirect:/login";
        }

        try {
            gameService.submitGuess(userId(session), sessionId, guess);
        } catch (InvalidGuessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return "redirect:/player/home";
        }
        return "redirect:/player/game/" + sessionId;
    }

    private boolean isPlayer(HttpSession session) {
        return session.getAttribute(SessionAttributes.USER_ID) instanceof Number
                && session.getAttribute(SessionAttributes.USERNAME) != null
                && "PLAYER".equals(session.getAttribute(SessionAttributes.ROLE));
    }

    private long userId(HttpSession session) {
        return ((Number) session.getAttribute(SessionAttributes.USER_ID)).longValue();
    }
}
