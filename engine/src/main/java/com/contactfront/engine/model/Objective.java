package com.contactfront.engine.model;

public final class Objective {
    public final String id;
    public final String name;
    public int x;
    public int y;
    public final String type;
    public boolean required;

    public Objective(String id, String name, int x, int y, String type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
        this.required = true;
    }
}
