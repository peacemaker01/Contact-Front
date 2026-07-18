package com.contactfront.engine.model;

public final class Objective {
    public final String id;
    public final String name;
    public int x;
    public int y;
    public final String type;
    public boolean required;
    public int requiredTurns = 3;
    public int targetUnitId = 0;

    public Objective(String id, String name, int x, int y, String type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
        this.required = true;
    }
    
    public Objective(String id, String name, int x, int y, String type, int requiredTurns) {
        this(id, name, x, y, type);
        this.requiredTurns = requiredTurns;
    }
    
    public Objective(String id, String name, int targetUnitId, String type) {
        this(id, name, 0, 0, type);
        this.targetUnitId = targetUnitId;
    }
}
