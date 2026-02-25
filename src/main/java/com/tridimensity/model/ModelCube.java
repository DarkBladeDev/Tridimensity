package com.tridimensity.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.UUID;
import java.util.Map;

public class ModelCube {
    private final String name;
    private final UUID uuid;
    private final Vector3f from;
    private final Vector3f to;
    private final Map<String, ModelFace> faces;
    private final Vector3f localOffset;
    private final Vector3f localScale;
    private final Quaternionf localRotation;
    private final Vector3f pivot;

    public ModelCube(UUID uuid, String name, Vector3f from, Vector3f to, Map<String, ModelFace> faces, Vector3f localOffset, Vector3f localScale, Quaternionf localRotation, Vector3f pivot) {
        this.uuid = uuid;
        this.name = name;
        this.from = from;
        this.to = to;
        this.faces = faces;
        this.localOffset = localOffset;
        this.localScale = localScale;
        this.localRotation = localRotation;
        this.pivot = pivot;
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

    public Vector3f getSize() {
        return new Vector3f(to).sub(from);
    }

    public Vector3f getCenter() {
        return new Vector3f(from).add(to).mul(0.5f);
    }

    public Map<String, ModelFace> getFaces() {
        return faces;
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

    public Vector3f getPivot() {
        return new Vector3f(pivot);
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
