package com.contactfront.engine.model;

public enum Stance {
    AGGRESSIVE(10, 0.5, 1.5, 1.3, 1.0),
    DEFENSIVE(-5, 1.5, 0.7, 0.7, 1.0),
    OVERWATCH(0, 1.2, 1.0, 1.0, 1.0);

    public final int accuracyMod;
    public final double coverReceivedMult;
    public final double outgoingSuppressionMult;
    public final double incomingSuppressionMult;
    public final double moveMult;

    Stance(int accuracyMod, double coverReceivedMult, double outgoingSuppressionMult,
           double incomingSuppressionMult, double moveMult) {
        this.accuracyMod = accuracyMod;
        this.coverReceivedMult = coverReceivedMult;
        this.outgoingSuppressionMult = outgoingSuppressionMult;
        this.incomingSuppressionMult = incomingSuppressionMult;
        this.moveMult = moveMult;
    }
}
