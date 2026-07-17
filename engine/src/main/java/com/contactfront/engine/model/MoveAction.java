package com.contactfront.engine.model;

public record MoveAction(int unitId, int targetX, int targetY) implements Action {}
