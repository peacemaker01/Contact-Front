package com.contactfront.engine.model;

import com.contactfront.engine.model.Obstacle.ObstacleType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObstacleTest {

    @Test
    void singleTileObstacle() {
        Obstacle obs = new Obstacle(5, 7, ObstacleType.MINEFIELD);
        assertEquals(5, obs.x());
        assertEquals(7, obs.y());
        assertEquals(ObstacleType.MINEFIELD, obs.type());
        assertEquals(1, obs.footprint().size());
        assertEquals(5.0, obs.footprint().get(0)[0]);
        assertEquals(7.0, obs.footprint().get(0)[1]);
    }

    @Test
    void multiTileObstacle() {
        List<double[]> footprint = List.of(
            new double[]{0, 0},
            new double[]{1, 0},
            new double[]{0, 1}
        );
        Obstacle obs = new Obstacle(footprint, ObstacleType.FENCE);
        assertEquals(3, obs.footprint().size());
        assertEquals(ObstacleType.FENCE, obs.type());
        assertEquals(0, obs.x());
        assertEquals(0, obs.y());
    }

    @Test
    void minefieldMovementCostMultiplier() {
        assertEquals(3.0, ObstacleType.MINEFIELD.movementCostMultiplier);
    }

    @Test
    void minefieldDamageFactor() {
        assertEquals(999.0, ObstacleType.MINEFIELD.damageFactor);
    }

    @Test
    void fenceMovementCostMultiplier() {
        assertEquals(1.5, ObstacleType.FENCE.movementCostMultiplier);
    }

    @Test
    void fenceDamageFactor() {
        assertEquals(2.0, ObstacleType.FENCE.damageFactor);
    }

    @Test
    void barrierMovementCostMultiplier() {
        assertEquals(2.0, ObstacleType.BARRIER.movementCostMultiplier);
    }

    @Test
    void boobytrapDamageFactor() {
        assertEquals(100.0, ObstacleType.BOOBYTRAP.damageFactor);
    }

    @Test
    void allObstacleTypesExist() {
        ObstacleType[] types = ObstacleType.values();
        assertEquals(4, types.length);
        assertNotNull(ObstacleType.valueOf("MINEFIELD"));
        assertNotNull(ObstacleType.valueOf("FENCE"));
        assertNotNull(ObstacleType.valueOf("BARRIER"));
        assertNotNull(ObstacleType.valueOf("BOOBYTRAP"));
    }
}