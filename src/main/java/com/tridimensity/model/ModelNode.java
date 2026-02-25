package com.tridimensity.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelNode {
    private final String name;
    private final UUID uuid;
    private final Vector3f origin;
    private final Vector3f position;
    private final Vector3f rotation;
    private final Vector3f scale;
    private final Vector3f pivot;
    private final Vector3f localOffset;
    private final Vector3f localScale;
    private final Quaternionf localRotation;
    private final List<ModelNode> children;
    private final List<ModelCube> cubes;

    public ModelNode(String name, Vector3f origin, Vector3f position, Vector3f rotation, Vector3f scale) {
        this(name, origin, position, rotation, scale, new Vector3f(0,0,0), new Vector3f(0,0,0), new Vector3f(1,1,1), new Quaternionf().identity());
    }

    public ModelNode(String name, Vector3f origin, Vector3f position, Vector3f rotation, Vector3f scale, Vector3f pivot, Vector3f localOffset, Vector3f localScale, Quaternionf localRotation) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.origin = origin != null ? origin : new Vector3f(0, 0, 0);
        this.position = position != null ? position : new Vector3f(0, 0, 0);
        this.rotation = rotation != null ? rotation : new Vector3f(0, 0, 0);
        this.scale = scale != null ? scale : new Vector3f(1, 1, 1);
        this.pivot = pivot != null ? pivot : new Vector3f(0,0,0);
        this.localOffset = localOffset != null ? localOffset : new Vector3f(0,0,0);
        this.localScale = localScale != null ? localScale : new Vector3f(1,1,1);
        this.localRotation = localRotation != null ? localRotation : new Quaternionf().identity();
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

    public UUID getUuid() {
        return uuid;
    }

    public Vector3f getOrigin() {
        return new Vector3f(origin);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getRotation() {
        return new Vector3f(rotation);
    }

    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    public Vector3f getPivot() {
        return new Vector3f(pivot);
    }

    public Vector3f getLocalOffset() {
        return new Vector3f(localOffset);
    }

    public Vector3f getLocalScale() {
        return new Vector3f(localScale);
    }

    public Quaternionf getLocalRotation() {
        return new Quaternionf(localRotation);
    }

    public List<ModelNode> getChildren() {
        return new ArrayList<>(children);
    }

    public List<ModelCube> getCubes() {
        return new ArrayList<>(cubes);
    }
}
