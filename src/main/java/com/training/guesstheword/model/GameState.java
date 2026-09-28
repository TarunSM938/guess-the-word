package com.training.guesstheword.model;

import java.util.List;

public record GameState(
        long sessionId,
        String status,
        List<GuessRecord> guesses,
        int guessesRemaining,
        String answer) {
    public GameState {
        guesses = List.copyOf(guesses);
    }
}
