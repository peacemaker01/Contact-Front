package com.contactfront.engine.model;

/** Result of issuing an {@link Action} through the engine. */
public class ActionResult {
    public boolean accepted = false;
    public String reason = "";
    public boolean delayed = false;   // queued for a future turn (e.g. radio range)
    public boolean turnConsumed = false;
    public boolean ended = false;

    public static ActionResult ok() {
        ActionResult r = new ActionResult();
        r.accepted = true;
        r.turnConsumed = true;
        return r;
    }

    public static ActionResult delayed() {
        ActionResult r = new ActionResult();
        r.accepted = true;
        r.delayed = true;
        return r;
    }

    public static ActionResult reject(String reason) {
        ActionResult r = new ActionResult();
        r.accepted = false;
        r.reason = reason;
        return r;
    }
}
