package com.contactfront.ui.assets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GLTFLoader {
    private GLTFLoader() {}

    public record GLTFMesh(String name, int[] positions, int[] normals, int[] uvs, int vertexCount) {}
    public record GLTFMaterial(String name, String albedoTexture, String normalTexture, 
                                  String roughnessTexture, String metallicTexture) {}
    public record GLTFModel(GLTFMesh[] meshes, GLTFMaterial[] materials, String name) {}

    public static GLTFModel loadGLTF(String path) throws IOException {
        Path file = AssetLoader.getAssetRoot().resolve(path);
        if (!Files.exists(file)) {
            file = Path.of("assets/" + path);
        }
        return loadFromFile(file);
    }

    private static GLTFModel loadFromFile(Path file) throws IOException {
        String name = file.getFileName().toString();
        
        if (name.endsWith(".glb")) {
            return loadGLB(file);
        }
        
        return loadAsPlaceholder(name);
    }

    private static GLTFModel loadGLB(Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        ByteBuffer bb = ByteBuffer.wrap(data);
        
        int magic = bb.getInt();
        if (magic != 0x46546C67) {
            throw new IOException("Invalid GLB magic number");
        }
        
        int version = bb.getInt();
        int length = bb.getInt();
        
        return new GLTFModel(new GLTFMesh[0], new GLTFMaterial[0], file.getFileName().toString());
    }

    private static GLTFModel loadAsPlaceholder(String name) {
        GLTFMesh mesh = new GLTFMesh(name, new int[0], new int[0], new int[0], 0);
        GLTFMaterial mat = new GLTFMaterial(name, "placeholder_albedo.png", "placeholder_normal.png", 
                                              "placeholder_roughness.png", "placeholder_metallic.png");
        return new GLTFModel(new GLTFMesh[]{mesh}, new GLTFMaterial[]{mat}, name);
    }
}