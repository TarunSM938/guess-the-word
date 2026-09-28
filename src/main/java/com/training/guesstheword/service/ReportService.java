package com.training.guesstheword.service;

import com.training.guesstheword.model.DayReport;
import com.training.guesstheword.model.UserDayRow;
import com.training.guesstheword.model.User;
import com.training.guesstheword.repository.ReportRepository;
import com.training.guesstheword.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    public DayReport getDayReport(String date) {
        final LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("Enter a valid date");
        }
        return reportRepository.dayReport(parsedDate.toString());
    }

    public List<UserDayRow> getUserReport(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No user with that username"));
        return reportRepository.userReport(user.getId());
    }
}
