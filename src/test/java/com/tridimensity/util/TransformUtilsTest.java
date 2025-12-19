package com.tridimensity.util;

import com.tridimensity.io.BlockbenchLoader;
import com.tridimensity.model.Model;
import com.tridimensity.model.ModelInstance;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TransformUtilsTest {

    @Test
    void identityDecomposition() {
        Matrix4f m = new Matrix4f().identity();
        TransformUtils.Parts p = TransformUtils.decompose(m);
        assertEquals(0.0f, p.translation.x, 1e-6f);
        assertEquals(0.0f, p.translation.y, 1e-6f);
        assertEquals(0.0f, p.translation.z, 1e-6f);
        Vector3f v = new Vector3f(1,0,0);
        p.rotation.transform(v);
        assertEquals(1.0f, v.x, 1e-6f);
        assertEquals(0.0f, v.y, 1e-6f);
        assertEquals(0.0f, v.z, 1e-6f);
        assertEquals(1.0f, p.scale.x, 1e-6f);
        assertEquals(1.0f, p.scale.y, 1e-6f);
        assertEquals(1.0f, p.scale.z, 1e-6f);
    }

    @Test
    void rotationY90Decomposition() {
        String json = """
            {
                "elements": [],
                "outliner": [
                    {
                        "name": "pivoted",
                        "origin": [8, 8, 8],
                        "rotation": [0, 90, 0],
                        "children": []
                    }
                ]
            }
            """;
        Model model = BlockbenchLoader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        ModelInstance instance = model.instantiate();
        Matrix4f m = instance.computeWorldTransforms().get(model.getRoots().get(0));
        Quaternionf q = TransformUtils.extractRotation(m);
        Vector3f x = new Vector3f(1,0,0);
        q.transform(x);
        assertEquals(0.0f, x.x, 1e-6f);
        assertEquals(0.0f, x.y, 1e-6f);
        assertEquals(1.0f, x.z, 1e-6f);
    }
}
