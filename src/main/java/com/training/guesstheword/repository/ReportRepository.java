package com.training.guesstheword.repository;

import com.training.guesstheword.model.DayReport;
import com.training.guesstheword.model.UserDayRow;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DayReport dayReport(String date) {
        return jdbcTemplate.queryForObject(
                "SELECT ? AS report_date,"
                        + " COALESCE(COUNT(DISTINCT user_id), 0) AS users_played,"
                        + " COALESCE(SUM(CASE WHEN status = ? THEN 1 ELSE 0 END), 0)"
                        + " AS correct_guesses"
                        + " FROM game_sessions WHERE played_on = ?",
                (resultSet, rowNumber) -> new DayReport(
                        resultSet.getString("report_date"),
                        resultSet.getInt("users_played"),
                        resultSet.getInt("correct_guesses")),
                date,
                "WON",
                date);
    }

    public List<UserDayRow> userReport(long userId) {
        return jdbcTemplate.query(
                "SELECT played_on AS report_date, COUNT(*) AS words_tried,"
                        + " COALESCE(SUM(CASE WHEN status = ? THEN 1 ELSE 0 END), 0)"
                        + " AS correct_guesses"
                        + " FROM game_sessions WHERE user_id = ?"
                        + " GROUP BY played_on ORDER BY played_on DESC",
                (resultSet, rowNumber) -> new UserDayRow(
                        resultSet.getString("report_date"),
                        resultSet.getInt("words_tried"),
                        resultSet.getInt("correct_guesses")),
                "WON",
                userId);
    }
}
