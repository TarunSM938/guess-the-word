package com.training.guesstheword.controller;

import com.training.guesstheword.model.User;
import com.training.guesstheword.service.UserService;
import com.training.guesstheword.service.UserValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        if (session.getAttribute(SessionAttributes.USERNAME) == null) {
            return "redirect:/login";
        }
        return homeForRole((String) session.getAttribute(SessionAttributes.ROLE));
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmPassword,
            Model model) {
        if (password == null || !password.equals(confirmPassword)) {
            model.addAttribute("errors", List.of("Passwords do not match."));
            model.addAttribute("username", username == null ? "" : username);
            return "register";
        }

        try {
            userService.register(username, password);
        } catch (UserValidationException exception) {
            model.addAttribute("errors", exception.getErrors());
            model.addAttribute("username", username == null ? "" : username);
            return "register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String loginForm(
            @RequestParam(required = false) String registered,
            Model model) {
        if (registered != null) {
            model.addAttribute("registered", true);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            HttpServletRequest request,
            Model model) {
        Optional<User> authenticatedUser = userService.authenticate(username, password);
        if (authenticatedUser.isEmpty()) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", username == null ? "" : username);
            return "login";
        }

        User user = authenticatedUser.get();
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(SessionAttributes.USER_ID, user.getId());
        session.setAttribute(SessionAttributes.USERNAME, user.getUsername());
        session.setAttribute(SessionAttributes.ROLE, user.getRole());
        return homeForRole(user.getRole());
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String homeForRole(String role) {
        if ("ADMIN".equals(role)) {
            return "redirect:/admin/home";
        }
        if ("PLAYER".equals(role)) {
            return "redirect:/player/home";
        }
        return "redirect:/login";
    }
}
