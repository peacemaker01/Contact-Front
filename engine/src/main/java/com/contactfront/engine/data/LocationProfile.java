package com.contactfront.engine.data;

import java.util.List;

public record LocationProfile(
    String name,
    BoundingBox boundingBox,
    List<String> tags,
    long seedHint
) {
    public record BoundingBox(
        double minLat,
        double maxLat,
        double minLon,
        double maxLon
    ) {
        public double centerLat() { return (minLat + maxLat) / 2.0; }
        public double centerLon() { return (minLon + maxLon) / 2.0; }
        public double width() { return maxLon - minLon; }
        public double height() { return maxLat - minLat; }
    }
}