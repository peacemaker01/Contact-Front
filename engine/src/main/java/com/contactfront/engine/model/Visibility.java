package com.contactfront.engine.model;

public enum Visibility {
    UNSEEN(0),
    PREVIOUSLY_SEEN(1),
    VISIBLE(2);

    public final int code;

    Visibility(int code) {
        this.code = code;
    }

    public static Visibility of(int code) {
        return switch (code) {
            case 2 -> VISIBLE;
            case 1 -> PREVIOUSLY_SEEN;
            default -> UNSEEN;
        };
    }
}
