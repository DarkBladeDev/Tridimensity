package com.tridimensity.io;

import com.tridimensity.model.Model;
import com.tridimensity.model.ModelInstance;
import com.tridimensity.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExampleModelTest {

    @Test
    void exampleModelAutoFixProducesGroupRotation() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/models/example.bbmodel")) {
            assertNotNull(stream);
            var options = new com.tridimensity.io.options.ParserOptions(true);
            Model model = BlockbenchLoader.load(stream, options);
            assertNotNull(model);
            assertTrue(model.getRoots().size() >= 1);

            assertTrue(findNodeByNameWithYaw(model, "BLUE_CONCRETE", -45.0f));
            assertTrue(findNodeByNameWithYaw(model, "ORANGE_CONCRETE", -45.0f));

            ModelInstance instance = model.instantiate();
            Map<ModelNode, Matrix4f> world = instance.computeWorldTransforms();

            ModelNode blue = findNodeByName(model, "BLUE_CONCRETE");
            assertNotNull(blue);
            Vector3f origin = blue.getOrigin();
            assertEquals(8.24359f, origin.x, 0.0001f);
            assertEquals(1.05f, origin.y, 0.0001f);
            assertEquals(3.60641f, origin.z, 0.0001f);
            assertTrue(world.containsKey(blue));
        }
    }

    private static boolean findNodeByNameWithYaw(Model model, String name, float expectedYaw) {
        ModelNode node = findNodeByName(model, name);
        if (node == null) return false;
        return Math.abs(node.getRotation().y - expectedYaw) < 0.0001f;
    }

    private static ModelNode findNodeByName(Model model, String name) {
        for (ModelNode root : model.getRoots()) {
            ModelNode found = findNodeRecursive(root, name);
            if (found != null) return found;
        }
        return null;
    }

    private static ModelNode findNodeRecursive(ModelNode node, String name) {
        if (name.equals(node.getName())) return node;
        for (ModelNode child : node.getChildren()) {
            ModelNode f = findNodeRecursive(child, name);
            if (f != null) return f;
        }
        return null;
    }
}
