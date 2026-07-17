package com.contactfront.engine.model;

/** An AI-scheduled area strike, queued in the delayed-order pipeline. */
public record EnemyStrike(String kind, int x, int y, int rounds) implements Command {
    public static final String ARTY = "ARTY";
    public static final String CAS = "CAS";
}
