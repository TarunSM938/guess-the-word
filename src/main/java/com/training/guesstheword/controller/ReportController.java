package com.training.guesstheword.controller;

import com.training.guesstheword.model.DayReport;
import com.training.guesstheword.model.UserDayRow;
import com.training.guesstheword.service.ReportService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports/day")
    public String dayReport(
            @RequestParam(required = false) String date,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String selectedDate = date == null || date.isBlank() ? LocalDate.now().toString() : date;
        model.addAttribute("date", selectedDate);
        try {
            DayReport report = reportService.getDayReport(selectedDate);
            model.addAttribute("report", report);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "report-day";
    }

    @GetMapping("/admin/reports/user")
    public String userReport(
            @RequestParam(required = false) String username,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("username", username == null ? "" : username);
        model.addAttribute("rows", List.of());
        model.addAttribute("submitted", false);
        if (username == null || username.isBlank()) {
            return "report-user";
        }

        try {
            model.addAttribute("rows", reportService.getUserReport(username.trim()));
            model.addAttribute("submitted", true);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "report-user";
    }

    private boolean isAdmin(HttpSession session) {
        return session.getAttribute(SessionAttributes.USER_ID) instanceof Number
                && session.getAttribute(SessionAttributes.USERNAME) != null
                && "ADMIN".equals(session.getAttribute(SessionAttributes.ROLE));
    }
}
