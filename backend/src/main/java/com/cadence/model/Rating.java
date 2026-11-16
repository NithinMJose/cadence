package com.cadence.model;

/**
 * FSRS evaluation grade submitted by user after review.
 */
public enum Rating {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    private final int value;

    Rating(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Rating fromValue(int value) {
        for (Rating r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        throw new IllegalArgumentException(
                "Invalid rating value: " + value + ". Must be 1 (Again), 2 (Hard), 3 (Good), or 4 (Easy)."
        );
    }
}
