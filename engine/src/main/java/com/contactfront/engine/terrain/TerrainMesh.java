package com.contactfront.engine.terrain;

import com.contactfront.engine.model.GameState;
import com.contactfront.engine.model.Terrain;
import com.contactfront.engine.model.Tile;

import java.util.ArrayList;
import java.util.List;

public final class TerrainMesh {
    private TerrainMesh() {}

    public record Vertex(double x, double y, double z, double nx, double ny, double nz, int terrainType) {}
    public record Mesh(List<Vertex> vertices, List<Integer> indices, List<Terrain> terrainTypes) {}

    public static Mesh generate(GameState state, double maxHeight) {
        if (state.elevation == null) {
            return generateFlat(state);
        }

        int w = state.width();
        int h = state.height();
        List<Vertex> vertices = new ArrayList<>(w * h);
        List<Integer> indices = new ArrayList<>(w * h * 6);
        List<Terrain> terrainTypes = new ArrayList<>(w * h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double z = state.elevation[y][x] * maxHeight;
                vertices.add(new Vertex(x, y, z, 0, 1, 0, state.grid[y][x].type.ordinal()));
                terrainTypes.add(state.grid[y][x].type);
            }
        }

        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                int i00 = y * w + x;
                int i10 = y * w + (x + 1);
                int i01 = (y + 1) * w + x;
                int i11 = (y + 1) * w + (x + 1);

                indices.add(i00);
                indices.add(i01);
                indices.add(i10);

                indices.add(i10);
                indices.add(i01);
                indices.add(i11);
            }
        }

        computeNormals(vertices, indices, w, h);
        return new Mesh(vertices, indices, terrainTypes);
    }

    private static Mesh generateFlat(GameState state) {
        int w = state.width();
        int h = state.height();
        List<Vertex> vertices = new ArrayList<>(w * h);
        List<Integer> indices = new ArrayList<>(w * h * 6);
        List<Terrain> terrainTypes = new ArrayList<>(w * h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                vertices.add(new Vertex(x, y, 0, 0, 1, 0, state.grid[y][x].type.ordinal()));
                terrainTypes.add(state.grid[y][x].type);
            }
        }

        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                int i00 = y * w + x;
                int i10 = y * w + (x + 1);
                int i01 = (y + 1) * w + x;
                int i11 = (y + 1) * w + (x + 1);

                indices.add(i00);
                indices.add(i01);
                indices.add(i10);

                indices.add(i10);
                indices.add(i01);
                indices.add(i11);
            }
        }

        return new Mesh(vertices, indices, terrainTypes);
    }

    private static void computeNormals(List<Vertex> vertices, List<Integer> indices, int w, int h) {
        int n = vertices.size();
        double[] nx = new double[n];
        double[] ny = new double[n];
        double[] nz = new double[n];

        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vertex v0 = vertices.get(i0);
            Vertex v1 = vertices.get(i1);
            Vertex v2 = vertices.get(i2);

            double dx1 = v1.x - v0.x;
            double dy1 = v1.y - v0.y;
            double dz1 = v1.z - v0.z;
            double dx2 = v2.x - v0.x;
            double dy2 = v2.y - v0.y;
            double dz2 = v2.z - v0.z;

            double cx = dy1 * dz2 - dz1 * dy2;
            double cy = dz1 * dx2 - dx1 * dz2;
            double cz = dx1 * dy2 - dy1 * dx2;

            double len = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (len > 0) {
                cx /= len;
                cy /= len;
                cz /= len;
            }

            nx[i0] += cx; ny[i0] += cy; nz[i0] += cz;
            nx[i1] += cx; ny[i1] += cy; nz[i1] += cz;
            nx[i2] += cx; ny[i2] += cy; nz[i2] += cz;
        }

        for (int i = 0; i < n; i++) {
            double len = Math.sqrt(nx[i] * nx[i] + ny[i] * ny[i] + nz[i] * nz[i]);
            if (len > 0) {
                vertices.set(i, new Vertex(
                    vertices.get(i).x(), vertices.get(i).y(), vertices.get(i).z(),
                    nx[i] / len, ny[i] / len, nz[i] / len,
                    vertices.get(i).terrainType()
                ));
            }
        }
    }
}