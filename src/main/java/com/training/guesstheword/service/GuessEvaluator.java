package com.training.guesstheword.service;

public class GuessEvaluator {
    private static final int WORD_LENGTH = 5;

    public String evaluate(String answer, String guess) {
        if (answer == null || guess == null
                || answer.length() != WORD_LENGTH || guess.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("Answer and guess must each be exactly 5 characters.");
        }

        char[] result = new char[WORD_LENGTH];
        int[] remainingLetters = new int[26];

        for (int position = 0; position < WORD_LENGTH; position++) {
            if (answer.charAt(position) == guess.charAt(position)) {
                result[position] = 'G';
            } else {
                remainingLetters[answer.charAt(position) - 'A']++;
            }
        }

        for (int position = 0; position < WORD_LENGTH; position++) {
            if (result[position] == 'G') {
                continue;
            }

            int letterIndex = guess.charAt(position) - 'A';
            if (remainingLetters[letterIndex] > 0) {
                result[position] = 'Y';
                remainingLetters[letterIndex]--;
            } else {
                result[position] = 'X';
            }
        }

        return new String(result);
    }
}
