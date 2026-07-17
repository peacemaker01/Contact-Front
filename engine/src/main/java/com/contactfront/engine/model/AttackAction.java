package com.contactfront.engine.model;

public record AttackAction(int unitId, int targetUnitId) implements Action {}
