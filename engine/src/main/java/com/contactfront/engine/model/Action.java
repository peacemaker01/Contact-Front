package com.contactfront.engine.model;

/**
 * Sealed command type. Every player order in v1 is one of the record types
 * declared alongside this interface (MoveAction, AttackAction, ReconAction,
 * ResupplyAction, CallCasAction, SetStanceAction). Sealing keeps future
 * action types additive — they can only be added here, never invented by callers.
 */
public sealed interface Action extends Command
        permits MoveAction, AttackAction, ReconAction, ResupplyAction, CallCasAction, SetStanceAction, CallSmokeAction, CallArtilleryAction {

    /** The unit the order is issued to (0 == no specific unit, e.g. global CAS). */
    int unitId();
}
