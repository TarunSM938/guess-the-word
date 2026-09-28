package com.training.guesstheword.model;

public record GameSession(long id, long userId, long wordId, String status, String playedOn) {
}
