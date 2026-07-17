package com.contactfront.ui.assets;

public final class Material {
    private Material() {}

    public record PBRMaterial(
        String name,
        Texture.TextureData albedo,
        Texture.TextureData normal,
        double metallic,
        double roughness,
        double emissive
    ) {}

    public static PBRMaterial createPlaceholder(String name) {
        return new PBRMaterial(
            name,
            Texture.createPlaceholder(name + "_albedo"),
            Texture.createPlaceholder(name + "_normal"),
            0.0,
            0.5,
            0.0
        );
    }

    public static PBRMaterial createGrass() {
        return new PBRMaterial(
            "grass",
            Texture.createPlaceholder("grass_albedo"),
            Texture.createPlaceholder("grass_normal"),
            0.0,
            0.7,
            0.0
        );
    }

    public static PBRMaterial createDirt() {
        return new PBRMaterial(
            "dirt",
            Texture.createPlaceholder("dirt_albedo"),
            Texture.createPlaceholder("dirt_normal"),
            0.0,
            0.8,
            0.0
        );
    }

    public static PBRMaterial createRock() {
        return new PBRMaterial(
            "rock",
            Texture.createPlaceholder("rock_albedo"),
            Texture.createPlaceholder("rock_normal"),
            0.0,
            0.4,
            0.0
        );
    }

    public static PBRMaterial createWater() {
        return new PBRMaterial(
            "water",
            Texture.createPlaceholder("water_albedo"),
            Texture.createPlaceholder("water_normal"),
            0.0,
            0.1,
            0.2
        );
    }

    public static PBRMaterial createAsphalt() {
        return new PBRMaterial(
            "asphalt",
            Texture.createPlaceholder("asphalt_albedo"),
            Texture.createPlaceholder("asphalt_normal"),
            0.0,
            0.9,
            0.0
        );
    }

    public static PBRMaterial createBuilding() {
        return new PBRMaterial(
            "building",
            Texture.createPlaceholder("building_albedo"),
            Texture.createPlaceholder("building_normal"),
            0.0,
            0.3,
            0.0
        );
    }

    public static PBRMaterial createTree() {
        return new PBRMaterial(
            "tree",
            Texture.createPlaceholder("tree_albedo"),
            Texture.createPlaceholder("tree_normal"),
            0.0,
            0.6,
            0.0
        );
    }

    public static PBRMaterial createBush() {
        return new PBRMaterial(
            "bush",
            Texture.createPlaceholder("bush_albedo"),
            Texture.createPlaceholder("bush_normal"),
            0.0,
            0.7,
            0.0
        );
    }
}