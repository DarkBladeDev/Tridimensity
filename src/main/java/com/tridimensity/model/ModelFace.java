package com.tridimensity.model;

/**
 * Represents a face of a cube (Element) in the model.
 */
public class ModelFace {
    private final float[] uv; // [u1, v1, u2, v2]
    private final String texture; // Texture variable or path (#0, etc)
    private final int rotation; // UV rotation (0, 90, 180, 270)
    
    public ModelFace(float[] uv, String texture, int rotation) {
        this.uv = uv;
        this.texture = texture;
        this.rotation = rotation;
    }

    public float[] getUv() {
        return uv != null ? uv.clone() : null;
    }

    public String getTexture() {
        return texture;
    }
    
    public int getRotation() {
        return rotation;
    }
}
