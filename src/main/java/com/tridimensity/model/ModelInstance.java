package com.tridimensity.model;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class ModelInstance {

    private final Model model;

    public ModelInstance(Model model) {
        this.model = model;
    }

    public Map<ModelNode, Matrix4f> computeWorldTransforms() {
        Map<ModelNode, Matrix4f> results = new HashMap<>();
        Matrix4f identityMatrix = new Matrix4f().identity();
        Vector3f worldOrigin = new Vector3f(0, 0, 0);

        for (ModelNode root : model.getRoots()) {
            computeRecursive(root, identityMatrix, worldOrigin, results);
        }

        return results;
    }

    public Map<ModelNode, Vector3f> computeWorldPivotPositions() {
        Map<ModelNode, Matrix4f> world = computeWorldTransforms();
        Map<ModelNode, Vector3f> pivots = new HashMap<>();
        for (Map.Entry<ModelNode, Matrix4f> e : world.entrySet()) {
            Vector3f p = new Vector3f(e.getKey().getPivot());
            Vector3f worldPivot = new Vector3f(p);
            e.getValue().transformPosition(worldPivot);
            pivots.put(e.getKey(), worldPivot);
        }
        return pivots;
    }

    private void computeRecursive(
        ModelNode node,
        Matrix4f parentMatrix,
        Vector3f parentPivot,
        Map<ModelNode, Matrix4f> results
    ) {
        Vector3f position = node.getLocalOffset();
        Vector3f pivot = node.getPivot();
        Vector3f scale = node.getLocalScale();

        Vector3f offset = new Vector3f(pivot)
            .sub(parentPivot)
            .add(position);

        Matrix4f worldMatrix = new Matrix4f(parentMatrix);
        worldMatrix.translate(offset);
        worldMatrix.translate(pivot);
        worldMatrix.rotate(node.getLocalRotation());
        worldMatrix.scale(scale);
        worldMatrix.translate(new Vector3f(pivot).mul(-1f));

        results.put(node, worldMatrix);

        for (ModelNode child : node.getChildren()) {
            computeRecursive(child, worldMatrix, pivot, results);
        }
    }
}
