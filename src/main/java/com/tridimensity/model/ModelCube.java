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
    private final String name;
    private final UUID uuid;
    private final Vector3f from;
    private final Vector3f to;
    private final Map<String, ModelFace> faces; 

    public ModelCube(UUID uuid, String name, Vector3f from, Vector3f to, Map<String, ModelFace> faces) {
        this.uuid = uuid;
        this.name = name;
        this.from = from;
        this.to = to;
        this.faces = faces;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Vector3f getFrom() {
        return new Vector3f(from);
    }

    public Vector3f getTo() {
        return new Vector3f(to);
    }
    
    /**
     * @return The size of the cube (to - from).
     */
    public Vector3f getSize() {
        return new Vector3f(to).sub(from);
    }

    /**
     * @return The center point of the cube.
     */
    public Vector3f getCenter() {
        return new Vector3f(from).add(to).mul(0.5f);
    }

    public Map<String, ModelFace> getFaces() {
        return faces;
    }

    @Override
    public String toString() {
        return "ModelCube{" +
                "name=" + name +
                ", uuid=" + uuid +
                ", from=" + from +
                ", to=" + to +
                '}';
    }
}
