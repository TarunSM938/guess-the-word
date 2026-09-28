package com.training.guesstheword.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GuessEvaluatorTest {
    private final GuessEvaluator evaluator = new GuessEvaluator();

    @Test
    void marksExactGuessAsAllGreen() {
        assertEquals("GGGGG", evaluator.evaluate("CRANE", "CRANE"));
    }

    @Test
    void countsRepeatedLettersOnlyOnceWhenNotMatchedExactly() {
        assertEquals("YYGYX", evaluator.evaluate("APPLE", "PAPER"));
    }

    @Test
    void marksOnlyOneUnmatchedCopyOfRepeatedAnswerLetter() {
        assertEquals("XXYXG", evaluator.evaluate("CRANE", "EERIE"));
    }

    @Test
    void handlesRepeatedGuessLettersAgainstSingleAnswerLetter() {
        assertEquals("YXYXX", evaluator.evaluate("HOUSE", "SPEED"));
    }

    @Test
    void marksAbsentLettersAsGrey() {
        assertEquals("XXXXX", evaluator.evaluate("CRANE", "MOUTH"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(null, "CRANE"));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("CRANE", null));
    }

    @Test
    void rejectsWordsThatAreNotFiveCharacters() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate("CRANE", "CRAN"));
    }
}
