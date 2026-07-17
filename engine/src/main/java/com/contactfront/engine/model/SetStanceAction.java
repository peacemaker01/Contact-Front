package com.contactfront.engine.model;

public record SetStanceAction(int unitId, Stance stance) implements Action {}
