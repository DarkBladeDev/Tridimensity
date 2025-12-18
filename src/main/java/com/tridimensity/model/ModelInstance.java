package com.tridimensity.model;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a runtime instance of a Model.
 * Handles the calculation of world matrices for rendering.
 */
public class ModelInstance {

    private final Model model;
    private static final float SCALE_FACTOR = 1.0f / 16.0f;

    public ModelInstance(Model model) {
        this.model = model;
    }

    /**
     * Computes the world transformation matrices for all nodes in the model.
     * 
     * Formula per node:
     * M_local = T(pos) * T(pivot) * R(rot) * S(scale) * T(-pivot)
     * M_world = M_parent * M_local
     * 
     * All translation units (pos, pivot) are converted from pixels to blocks (1/16).
     * 
     * @return A map of Node -> World Matrix
     */
    public Map<ModelNode, Matrix4f> computeWorldTransforms() {
        Map<ModelNode, Matrix4f> results = new HashMap<>();
        
        // Identity for the "world" (or root parent)
        Matrix4f rootTransform = new Matrix4f().identity();

        for (ModelNode root : model.getRoots()) {
            computeRecursive(root, rootTransform, results);
        }

        return results;
    }

    private void computeRecursive(ModelNode node, Matrix4f parentMatrix, Map<ModelNode, Matrix4f> results) {
        // 1. Build Local Matrix
        Matrix4f local = new Matrix4f();

        // Data from node
        Vector3f pos = node.getPosition();
        Vector3f piv = node.getOrigin();
        Vector3f rot = node.getRotation();
        Vector3f scl = node.getScale();

        // Convert pixels to blocks
        float px = pos.x * SCALE_FACTOR;
        float py = pos.y * SCALE_FACTOR;
        float pz = pos.z * SCALE_FACTOR;

        float pivX = piv.x * SCALE_FACTOR;
        float pivY = piv.y * SCALE_FACTOR;
        float pivZ = piv.z * SCALE_FACTOR;

        // T(position)
        local.translate(px, py, pz);

        // T(pivot)
        local.translate(pivX, pivY, pivZ);

        // R(rotation) - Euler XYZ in degrees
        // JOML rotateXYZ takes radians
        local.rotateXYZ(
            (float) Math.toRadians(rot.x),
            (float) Math.toRadians(rot.y),
            (float) Math.toRadians(rot.z)
        );

        // S(scale)
        local.scale(scl);

        // T(-pivot)
        local.translate(-pivX, -pivY, -pivZ);

        // 2. Combine with Parent
        // M_world = M_parent * M_local
        Matrix4f world = new Matrix4f(parentMatrix).mul(local);

        // 3. Store result
        results.put(node, world);

        // 4. Recurse
        for (ModelNode child : node.getChildren()) {
            computeRecursive(child, world, results);
        }
    }
}
