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
     * Transformation hierarchy:
     * - Each node has a pivot point (origin) in global Blockbench space
     * - Child offset = (child_pivot - parent_pivot) + position_offset
     * - Rotation happens around the node's own pivot (implicit local origin)
     * - Scale is applied after rotation
     * 
     * Formula per node:
     * M_local = T(offset) * R(rotation) * S(scale)
     * M_world = M_parent * M_local
     * 
     * @return A map of Node -> World Matrix
     */
    public Map<ModelNode, Matrix4f> computeWorldTransforms() {
        Map<ModelNode, Matrix4f> results = new HashMap<>();
        
        // Identity matrix for root level
        Matrix4f identityMatrix = new Matrix4f().identity();
        Vector3f worldOrigin = new Vector3f(0, 0, 0);

        for (ModelNode root : model.getRoots()) {
            computeRecursive(root, identityMatrix, worldOrigin, results);
        }

        return results;
    }

    /**
     * Recursively computes world matrices for a node and its children.
     * 
     * @param node The current node
     * @param parentMatrix The accumulated world matrix of the parent
     * @param parentPivot The pivot point of the parent in Blockbench global space
     * @param results Output map to store computed matrices
     */
    private void computeRecursive(
        ModelNode node, 
        Matrix4f parentMatrix, 
        Vector3f parentPivot, 
        Map<ModelNode, Matrix4f> results
    ) {
        // Get node properties
        Vector3f position = node.getPosition();  // Animation/offset position
        Vector3f pivot = node.getOrigin();       // Pivot point (global Blockbench coords)
        Vector3f rotation = node.getRotation();  // Euler angles in degrees
        Vector3f scale = node.getScale();        // Scale factors

        // STEP 1: Calculate offset from parent pivot to this node's pivot
        // offset = (child_pivot - parent_pivot) + position
        Vector3f offset = new Vector3f(pivot)
            .sub(parentPivot)
            .add(position);
        
        // Convert from Blockbench pixels to Minecraft blocks
        offset.mul(SCALE_FACTOR);

        // STEP 2: Build world transformation matrix with pivot-centered rotation
        Vector3f pivotScaled = new Vector3f(pivot).mul(SCALE_FACTOR);
        Matrix4f worldMatrix = new Matrix4f(parentMatrix);
        worldMatrix.translate(offset);
        worldMatrix.translate(pivotScaled);
        worldMatrix.rotateXYZ(
            (float) Math.toRadians(rotation.x),
            (float) Math.toRadians(-rotation.y),
            (float) Math.toRadians(rotation.z)
        );
        worldMatrix.scale(scale);
        worldMatrix.translate(new Vector3f(pivotScaled).mul(-1f));
        
        // Store result
        results.put(node, worldMatrix);

        // STEP 4: Recurse to children
        // Pass this node's world matrix and pivot as the new parent context
        for (ModelNode child : node.getChildren()) {
            computeRecursive(child, worldMatrix, pivot, results);
        }
    }
}
