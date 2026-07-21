package com.contactfront.engine.model;

import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class TacticalGraphics {
    private final GeometryFactory gf = new GeometryFactory();

    public enum GraphicType {
        PHASE_LINE("Phase Line", "PL"),
        ASSAULT_AXIS("Assault Axis", "AA"),
        ASSEMBLY_AREA("Assembly Area", "AA"),
        ENTRY_POINT("Entry Point", "EP"),
        REFERENCE_POINT("Reference Point", "RP"),
        COVER_ARROW("Cover Arrow", "CA");

        public final String name;
        public final String shorthand;

        GraphicType(String name, String shorthand) {
            this.name = name;
            this.shorthand = shorthand;
        }
    }

    private final List<Graphic> graphics = new ArrayList<>();

    public void addLine(GraphicType type, List<double[]> points) {
        Coordinate[] coords = points.stream()
            .map(p -> new Coordinate(p[0], p[1]))
            .toArray(Coordinate[]::new);
        graphics.add(new Graphic(type, gf.createLineString(coords)));
    }

    public void addPolygon(GraphicType type, List<double[]> points) {
        if (points.size() < 3) return;
        Coordinate[] coords = new Coordinate[points.size() + 1];
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            coords[i] = new Coordinate(p[0], p[1]);
        }
        coords[coords.length - 1] = new Coordinate(points.get(0)[0], points.get(0)[1]); // Close ring
        graphics.add(new Graphic(type, gf.createPolygon(coords)));
    }

    public List<Graphic> graphics() { return graphics; }

    public List<Graphic> getAllGraphics() { return new ArrayList<>(graphics); }

    public static class Graphic {
        private final GraphicType type;
        private final Geometry geometry;

        public Graphic(GraphicType type, Geometry geometry) {
            this.type = type;
            this.geometry = geometry;
        }

        public GraphicType type() { return type; }
        public Geometry geometry() { return geometry; }

        public List<double[]> points() {
            List<double[]> result = new ArrayList<>();
            Coordinate[] coords = geometry.getCoordinates();
            for (Coordinate c : coords) {
                result.add(new double[]{c.x, c.y});
            }
            return result;
        }
    }
}