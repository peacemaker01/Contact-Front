package com.contactfront.engine.model;

/** An order queued for execution on a future turn (radio delay, CAS/arty lag). */
public class DelayedOrder {
    public final Command command;
    public final int executionTurn;
    public final int unitId;

    public DelayedOrder(Command command, int executionTurn, int unitId) {
        this.command = command;
        this.executionTurn = executionTurn;
        this.unitId = unitId;
    }
}
