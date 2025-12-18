package com.tridimensity.model;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Group in the Blockbench Outliner.
 * This is the primary unit of transformation (rotation/scale).
 */
public class ModelNode {
    private final String name;
    private final UUID uuid; // Groups also have UUIDs in newer Blockbench versions, but strictly we might just use name. 
                             // We'll generate one or use name if not present.
    private final Vector3f origin; // Pivot point
    private final Vector3f position; // Translation offset (optional, default 0)
    private final Vector3f rotation; // Euler angles in degrees
    private final Vector3f scale;    // Scale (optional, default 1)
    private final List<ModelNode> children;
    private final List<ModelCube> cubes;

    public ModelNode(String name, Vector3f origin, Vector3f position, Vector3f rotation, Vector3f scale) {
        this.name = name;
        this.uuid = UUID.randomUUID(); // Internal ID
        this.origin = origin != null ? origin : new Vector3f(0, 0, 0);
        this.position = position != null ? position : new Vector3f(0, 0, 0);
        this.rotation = rotation != null ? rotation : new Vector3f(0, 0, 0);
        this.scale = scale != null ? scale : new Vector3f(1, 1, 1);
        this.children = new ArrayList<>();
        this.cubes = new ArrayList<>();
    }

    public void addChild(ModelNode child) {
        this.children.add(child);
    }

    public void addCube(ModelCube cube) {
        this.cubes.add(cube);
    }

    public String getName() {
        return name;
    }

    /**
     * @return The pivot point (origin) in Blockbench coordinates (pixels).
     */
    public Vector3f getOrigin() {
        return new Vector3f(origin);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    /**
     * @return Rotation in degrees (Euler XYZ).
     */
    public Vector3f getRotation() {
        return new Vector3f(rotation);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    public List<ModelNode> getChildren() {
        return new ArrayList<>(children);
    }

    public List<ModelCube> getCubes() {
        return new ArrayList<>(cubes);
    }
}
