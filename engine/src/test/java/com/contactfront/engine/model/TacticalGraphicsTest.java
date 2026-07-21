package com.contactfront.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TacticalGraphicsTest {

    @Test
    void addLineStoresGraphic() {
        TacticalGraphics graphics = new TacticalGraphics();
        List<double[]> points = List.of(
            new double[]{0, 0},
            new double[]{5, 5},
            new double[]{10, 0}
        );
        graphics.addLine(TacticalGraphics.GraphicType.PHASE_LINE, points);
        assertEquals(1, graphics.getAllGraphics().size());
        assertEquals(TacticalGraphics.GraphicType.PHASE_LINE, graphics.getAllGraphics().get(0).type());
    }

    @Test
    void addPolygonStoresGraphic() {
        TacticalGraphics graphics = new TacticalGraphics();
        List<double[]> points = List.of(
            new double[]{0, 0},
            new double[]{5, 0},
            new double[]{5, 5},
            new double[]{0, 5}
        );
        graphics.addPolygon(TacticalGraphics.GraphicType.ASSEMBLY_AREA, points);
        assertEquals(1, graphics.getAllGraphics().size());
        assertEquals(TacticalGraphics.GraphicType.ASSEMBLY_AREA, graphics.getAllGraphics().get(0).type());
    }

    @Test
    void addPolygonWithLessThan3PointsDoesNothing() {
        TacticalGraphics graphics = new TacticalGraphics();
        List<double[]> points = List.of(
            new double[]{0, 0},
            new double[]{5, 5}
        );
        graphics.addPolygon(TacticalGraphics.GraphicType.PHASE_LINE, points);
        assertEquals(0, graphics.getAllGraphics().size());
    }

    @Test
    void graphicPointsReturnsCoordinates() {
        TacticalGraphics graphics = new TacticalGraphics();
        List<double[]> points = List.of(
            new double[]{1, 2},
            new double[]{3, 4},
            new double[]{5, 6}
        );
        graphics.addLine(TacticalGraphics.GraphicType.ASSAULT_AXIS, points);
        TacticalGraphics.Graphic graphic = graphics.getAllGraphics().get(0);
        List<double[]> result = graphic.points();
        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0)[0]);
        assertEquals(2.0, result.get(0)[1]);
        assertEquals(3.0, result.get(1)[0]);
        assertEquals(4.0, result.get(1)[1]);
    }

    @Test
    void graphicTypeHasNameAndShorthand() {
        assertEquals("Phase Line", TacticalGraphics.GraphicType.PHASE_LINE.name);
        assertEquals("PL", TacticalGraphics.GraphicType.PHASE_LINE.shorthand);
        assertEquals("Assault Axis", TacticalGraphics.GraphicType.ASSAULT_AXIS.name);
        assertEquals("AA", TacticalGraphics.GraphicType.ASSAULT_AXIS.shorthand);
    }

    @Test
    void allGraphicTypesExist() {
        TacticalGraphics.GraphicType[] types = TacticalGraphics.GraphicType.values();
        assertEquals(6, types.length);
        assertNotNull(TacticalGraphics.GraphicType.valueOf("PHASE_LINE"));
        assertNotNull(TacticalGraphics.GraphicType.valueOf("ASSAULT_AXIS"));
        assertNotNull(TacticalGraphics.GraphicType.valueOf("ASSEMBLY_AREA"));
        assertNotNull(TacticalGraphics.GraphicType.valueOf("ENTRY_POINT"));
        assertNotNull(TacticalGraphics.GraphicType.valueOf("REFERENCE_POINT"));
        assertNotNull(TacticalGraphics.GraphicType.valueOf("COVER_ARROW"));
    }

    @Test
    void graphicsListReturnsAllGraphics() {
        TacticalGraphics graphics = new TacticalGraphics();
        graphics.addLine(TacticalGraphics.GraphicType.PHASE_LINE, List.of(
            new double[]{0, 0}, new double[]{1, 1}
        ));
        graphics.addLine(TacticalGraphics.GraphicType.ASSAULT_AXIS, List.of(
            new double[]{2, 2}, new double[]{3, 3}
        ));
        assertEquals(2, graphics.graphics().size());
    }

    @Test
    void polygonIsClosed() {
        TacticalGraphics graphics = new TacticalGraphics();
        List<double[]> points = List.of(
            new double[]{0, 0},
            new double[]{2, 0},
            new double[]{2, 2},
            new double[]{0, 2}
        );
        graphics.addPolygon(TacticalGraphics.GraphicType.ASSEMBLY_AREA, points);
        TacticalGraphics.Graphic graphic = graphics.getAllGraphics().get(0);
        List<double[]> coords = graphic.points();
        // Polygon should be closed (first point repeated at end)
        assertEquals(5, coords.size());
        assertEquals(0.0, coords.get(0)[0]);
        assertEquals(0.0, coords.get(coords.size() - 1)[0]);
    }
}