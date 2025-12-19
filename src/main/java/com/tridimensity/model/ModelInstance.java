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
        Matrix4f rootBoneMatrix = new Matrix4f().identity();
        Vector3f rootOrigin = new Vector3f(0, 0, 0);

        for (ModelNode root : model.getRoots()) {
            computeRecursive(root, rootBoneMatrix, rootOrigin, results);
        }

        return results;
    }

    /**
     * @param parentBoneMatrix The accumulated transformation of the parent bone (without the T(-pivot) for geometry).
     * @param parentOrigin The pivot point of the parent in global Bind Pose coordinates.
     */
    private void computeRecursive(ModelNode node, Matrix4f parentBoneMatrix, Vector3f parentOrigin, Map<ModelNode, Matrix4f> results) {
        // Data from node
        Vector3f pos = node.getPosition(); // Animation/Offset position
        Vector3f piv = node.getOrigin();   // Pivot point (Global Bind Pose)
        Vector3f rot = node.getRotation();
        Vector3f scl = node.getScale();

        // Regla 1: origin está en espacio del padre (transformado).
        // PERO en Blockbench, 'origin' se define en coordenadas globales del editor (Bind Pose).
        // La jerarquía se construye con "offsets".
        // Offset = ChildPivot - ParentPivot + Position.
        // Esto es correcto para "Bone Parenting".
        
        // Calculate Relative Offset: (ChildPivot - ParentPivot) + Position
        Vector3f offset = new Vector3f(piv).sub(parentOrigin).add(pos);

        // Convert to blocks (Regla 7: Normalización previa)
        offset.mul(SCALE_FACTOR);
        
        // Construcción correcta con pivot centrando la rotación
        Matrix4f boneMatrix = new Matrix4f(parentBoneMatrix);
        boneMatrix.translate(offset);
        Vector3f pivotScaled = new Vector3f(piv).mul(SCALE_FACTOR);
        boneMatrix.translate(pivotScaled);
        boneMatrix.rotateXYZ(
            (float) Math.toRadians(rot.x),
            (float) Math.toRadians(-rot.y),
            (float) Math.toRadians(rot.z)
        );
        boneMatrix.scale(scl);
        boneMatrix.translate(new Vector3f(pivotScaled).mul(-1f));

        // Store final node matrix
        results.put(node, boneMatrix);

        // Recurse using the Bone Matrix
        for (ModelNode child : node.getChildren()) {
            computeRecursive(child, boneMatrix, piv, results);
        }
    }
}
