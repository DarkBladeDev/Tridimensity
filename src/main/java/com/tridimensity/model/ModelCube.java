package com.tridimensity.model;

import org.joml.Vector3f;
import java.util.UUID;
import java.util.Map;

/**
 * Represents a single cube (Element) in the model.
 * In Blockbench, these are leaf nodes of the geometry.
 * 
 * <p>Note: Individual cube rotation is explicitly not supported by this library
 * as per the specification. Rotations must happen at the Group (Node) level.</p>
 */
public class ModelCube {
    private final UUID uuid;
    private final Vector3f from;
    private final Vector3f to;
    // Map of face direction (north, south, etc.) to UV data. 
    // Keeping it generic for now as specific UV logic wasn't detailed, 
    // but the structure is needed.
    private final Map<String, Object> faces; 

    public ModelCube(UUID uuid, Vector3f from, Vector3f to, Map<String, Object> faces) {
        this.uuid = uuid;
        this.from = from;
        this.to = to;
        this.faces = faces;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Vector3f getFrom() {
        return new Vector3f(from);
    }

    public Vector3f getTo() {
        return new Vector3f(to);
    }

    public Map<String, Object> getFaces() {
        return faces;
    }

    @Override
    public String toString() {
        return "ModelCube{" +
                "uuid=" + uuid +
                ", from=" + from +
                ", to=" + to +
                '}';
    }
}
