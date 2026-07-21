package com.contactfront.engine.geo;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;

import java.io.File;

public final class TerrainRasterLoader {
    private TerrainRasterLoader() {}

    public static GridCoverage2D loadAndReproject(String filePath, String targetEpsgCode) {
        File rasterFile = new File(filePath);
        if (!rasterFile.exists()) {
            throw new IllegalArgumentException("Raster file not found: " + filePath);
        }
        try {
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            GridCoverage2DReader reader = new GeoTiffReader(rasterFile, hints);
            GridCoverage2D rawCoverage = reader.read(null);
            CoordinateReferenceSystem targetCRS = CRS.decode(targetEpsgCode);
            return (GridCoverage2D) Operations.DEFAULT.resample(rawCoverage, targetCRS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load/reproject raster: " + filePath, e);
        }
    }
}
