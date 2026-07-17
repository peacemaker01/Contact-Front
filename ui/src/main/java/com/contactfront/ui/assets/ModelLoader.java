package com.contactfront.ui.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModelLoader {
    private ModelLoader() {}

    public record Vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {}
    public record MeshData(List<Vertex> vertices, List<Integer> indices) {}
    public record Model(MeshData[] meshes, String name) {}

    public static Model loadModel(String name) throws IOException {
        Path file = AssetLoader.getAssetRoot().resolve(name);
        
        if (Files.exists(file)) {
            return loadFromFile(file);
        }
        
        InputStream is = ModelLoader.class.getResourceAsStream("/assets/" + name);
        if (is != null) {
            return loadFromResource(is, name);
        }
        
        return createPlaceholderModel(name);
    }

    private static Model loadFromFile(Path file) throws IOException {
        String ext = file.toString().toLowerCase();
        if (ext.endsWith(".obj")) {
            return loadObj(file);
        }
        if (ext.endsWith(".gltf") || ext.endsWith(".glb")) {
            throw new IOException("GLTF not yet implemented - use placeholder");
        }
        return createPlaceholderModel(file.getFileName().toString());
    }

    private static Model loadFromResource(InputStream is, String name) throws IOException {
        return createPlaceholderModel(name);
    }

    private static Model loadObj(Path file) throws IOException {
        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> vertexIndices = new ArrayList<>();
        List<Integer> normalIndices = new ArrayList<>();
        List<Integer> uvIndices = new ArrayList<>();

        List<String> lines = Files.readString(file).lines().toList();
        
        for (String line : lines) {
            if (line.startsWith("v ")) {
                String[] parts = line.substring(2).trim().split("\\s+");
                positions.add(Float.parseFloat(parts[0]));
                positions.add(Float.parseFloat(parts[1]));
                positions.add(Float.parseFloat(parts[2]));
            } else if (line.startsWith("vn ")) {
                String[] parts = line.substring(3).trim().split("\\s+");
                normals.add(Float.parseFloat(parts[0]));
                normals.add(Float.parseFloat(parts[1]));
                normals.add(Float.parseFloat(parts[2]));
            } else if (line.startsWith("vt ")) {
                String[] parts = line.substring(3).trim().split("\\s+");
                uvs.add(Float.parseFloat(parts[0]));
                uvs.add(Float.parseFloat(parts[1]));
            } else if (line.startsWith("f ")) {
                String[] parts = line.substring(2).trim().split("\\s+");
                for (String part : parts) {
                    String[] comps = part.split("/");
                    vertexIndices.add(Integer.parseInt(comps[0]) - 1);
                    if (comps.length > 1 && !comps[1].isEmpty()) {
                        uvIndices.add(Integer.parseInt(comps[1]) - 1);
                    }
                    if (comps.length > 2 && !comps[2].isEmpty()) {
                        normalIndices.add(Integer.parseInt(comps[2]) - 1);
                    }
                }
            }
        }

        List<Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < vertexIndices.size(); i++) {
            int vi = vertexIndices.get(i);
            float x = positions.get(vi * 3);
            float y = positions.get(vi * 3 + 1);
            float z = positions.get(vi * 3 + 2);
            
            float nx = 0, ny = 1, nz = 0;
            if (!normals.isEmpty() && i < normalIndices.size()) {
                int ni = normalIndices.get(i);
                nx = normals.get(ni * 3);
                ny = normals.get(ni * 3 + 1);
                nz = normals.get(ni * 3 + 2);
            }
            
            float u = 0, v = 0;
            if (!uvs.isEmpty() && i < uvIndices.size()) {
                int uvi = uvIndices.get(i);
                u = uvs.get(uvi * 2);
                v = uvs.get(uvi * 2 + 1);
            }
            
            vertices.add(new Vertex(x, y, z, nx, ny, nz, u, v));
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < vertexIndices.size(); i++) {
            indices.add(i);
        }

        MeshData mesh = new MeshData(vertices, indices);
        return new Model(new MeshData[]{mesh}, file.getFileName().toString());
    }

    public static Model createPlaceholderModel(String name) {
        List<Vertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        if (name.contains("tree") || name.contains("bush")) {
            for (int i = 0; i < 6; i++) {
                vertices.add(new Vertex(
                    (float)(Math.cos(i * Math.PI * 2 / 6) * 0.3),
                    0,
                    (float)(Math.sin(i * Math.PI * 2 / 6) * 0.3),
                    0, 1, 0,
                    (float)(i / 5.0f), 0
                ));
                indices.add(i);
            }
        } else if (name.contains("building")) {
            float[][] corners = {
                {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
                {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}
            };
            for (float[] corner : corners) {
                vertices.add(new Vertex(corner[0], corner[1], corner[2], 0, 1, 0, 0, 0));
            }
            for (int i = 0; i < 8; i++) {
                indices.add(i);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                vertices.add(new Vertex(
                    (i % 2) * 0.5f,
                    0,
                    (i / 2) * 0.5f,
                    0, 1, 0,
                    0, 0
                ));
                indices.add(i);
            }
        }

        MeshData mesh = new MeshData(vertices, indices);
        return new Model(new MeshData[]{mesh}, name);
    }
}