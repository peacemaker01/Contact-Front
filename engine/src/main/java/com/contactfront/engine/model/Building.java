package com.contactfront.engine.model;

import java.util.List;

public class Building {
    public final List<double[]> footprint;
    public final double height;

    public Building(List<double[]> footprint, double height) {
        this.footprint = footprint;
        this.height = height;
    }
}