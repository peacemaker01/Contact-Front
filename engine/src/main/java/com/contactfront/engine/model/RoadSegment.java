package com.contactfront.engine.model;

import java.util.List;

public record RoadSegment(List<double[]> points, RoadType type) {
    public enum RoadType {
        MOTORWAY, TRUNK, PRIMARY, SECONDARY, TERTIARY, UNCLASSIFIED, RESIDENTIAL, SERVICE
    }
}