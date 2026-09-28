package com.training.guesstheword.service;

import com.training.guesstheword.model.GameSession;
import com.training.guesstheword.model.GameState;
import com.training.guesstheword.model.GuessRecord;
import com.training.guesstheword.model.Word;
import com.training.guesstheword.repository.GameRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String WON = "WON";
    private static final String LOST = "LOST";

    private final GameRepository gameRepository;
    private final GuessEvaluator guessEvaluator = new GuessEvaluator();

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional
    public GameState startOrResume(long userId) {
        String today = LocalDate.now().toString();
        GameSession activeSession = gameRepository.findInProgressToday(userId, today).orElse(null);
        if (activeSession != null) {
            return buildState(activeSession);
        }

        if (gameRepository.countSessionsOn(userId, today) >= GameConstants.MAX_WORDS_PER_DAY) {
            throw new DailyLimitExceededException("You have used all 3 words for today.");
        }

        Word word = gameRepository.pickRandomWordNotUsedOn(userId, today)
                .orElseThrow(() -> new IllegalStateException("No unused words are available."));
        long sessionId = gameRepository.createSession(userId, word.id(), today);
        GameSession session = gameRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalStateException("The new game session could not be loaded."));
        return buildState(session);
    }

    @Transactional
    public GameState submitGuess(long userId, long sessionId, String rawGuess) {
        GameSession session = gameRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Game session does not exist."));
        if (session.userId() != userId) {
            throw new IllegalArgumentException("Game session does not belong to this user.");
        }
        if (!IN_PROGRESS.equals(session.status())) {
            throw new InvalidGuessException("This game is already finished");
        }

        String guess = rawGuess == null ? "" : rawGuess.trim().toUpperCase(Locale.ROOT);
        if (!guess.matches("[A-Z]{5}")) {
            throw new InvalidGuessException("Guess must be exactly 5 letters A-Z.");
        }

        String answer = gameRepository.findAnswer(session.wordId())
                .orElseThrow(() -> new IllegalStateException("The answer for this game could not be found."));
        List<GuessRecord> existingGuesses = gameRepository.findGuesses(sessionId);
        int guessNumber = existingGuesses.size() + 1;
        String resultPattern = guessEvaluator.evaluate(answer, guess);
        gameRepository.saveGuess(sessionId, guess, guessNumber, resultPattern);

        if (guess.equals(answer)) {
            gameRepository.updateStatus(sessionId, WON);
        } else if (guessNumber == GameConstants.MAX_GUESSES) {
            gameRepository.updateStatus(sessionId, LOST);
        }

        GameSession updatedSession = gameRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalStateException("The game session could not be reloaded."));
        return buildState(updatedSession);
    }

    public int wordsRemainingToday(long userId) {
        int sessionsPlayed = gameRepository.countSessionsOn(userId, LocalDate.now().toString());
        return Math.max(0, GameConstants.MAX_WORDS_PER_DAY - sessionsPlayed);
    }

    private GameState buildState(GameSession session) {
        List<GuessRecord> guesses = gameRepository.findGuesses(session.id());
        String answer = IN_PROGRESS.equals(session.status())
                ? null
                : gameRepository.findAnswer(session.wordId())
                        .orElseThrow(() -> new IllegalStateException(
                                "The answer for this game could not be found."));
        return new GameState(
                session.id(),
                session.status(),
                guesses,
                GameConstants.MAX_GUESSES - guesses.size(),
                answer);
    }
}
