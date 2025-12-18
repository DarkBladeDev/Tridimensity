package com.tridimensity.io;

import com.tridimensity.exception.ModelParseException;
import com.tridimensity.model.Model;
import com.tridimensity.model.ModelInstance;
import com.tridimensity.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BlockbenchLoaderTest {

    private Model load(String json) {
        return BlockbenchLoader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testValidSimpleModel() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "e005f561-1234-4567-89ab-cdef01234567",
                        "from": [0, 0, 0],
                        "to": [16, 16, 16],
                        "faces": {}
                    }
                ],
                "outliner": [
                    {
                        "name": "root",
                        "origin": [8, 0, 8],
                        "rotation": [0, 0, 0],
                        "children": [
                            "e005f561-1234-4567-89ab-cdef01234567"
                        ]
                    }
                ]
            }
            """;
        
        Model model = load(json);
        assertNotNull(model);
        assertEquals(1, model.getRoots().size());
        
        ModelNode root = model.getRoots().get(0);
        assertEquals("root", root.getName());
        assertEquals(new Vector3f(8, 0, 8), root.getOrigin());
        assertEquals(1, root.getCubes().size());
        assertEquals(UUID.fromString("e005f561-1234-4567-89ab-cdef01234567"), root.getCubes().get(0).getUuid());
    }

    @Test
    void testMissingOriginFails() {
        String json = """
            {
                "elements": [],
                "outliner": [
                    {
                        "name": "root",
                        "children": []
                    }
                ]
            }
            """;
        assertThrows(ModelParseException.class, () -> load(json));
    }

    @Test
    void testDuplicateUuidFails() {
        String json = """
            {
                "elements": [
                    { "uuid": "e005f561-1234-4567-89ab-cdef01234567", "from": [0,0,0], "to": [1,1,1], "faces": {} },
                    { "uuid": "e005f561-1234-4567-89ab-cdef01234567", "from": [0,0,0], "to": [1,1,1], "faces": {} }
                ],
                "outliner": []
            }
            """;
        assertThrows(ModelParseException.class, () -> load(json));
    }

    @Test
    void testTransformMath() {
        // Test a node shifted by 16 units (1 block) on X
        String json = """
            {
                "elements": [],
                "outliner": [
                    {
                        "name": "shifted",
                        "origin": [0, 0, 0],
                        "position": [16, 0, 0], 
                        "children": []
                    }
                ]
            }
            """;
        
        Model model = load(json);
        ModelInstance instance = model.instantiate();
        Map<ModelNode, Matrix4f> transforms = instance.computeWorldTransforms();
        
        ModelNode node = model.getRoots().get(0);
        Matrix4f mat = transforms.get(node);
        
        Vector3f translation = new Vector3f();
        mat.getTranslation(translation);
        
        // Should be 1.0 (16 pixels = 1 block)
        assertEquals(1.0f, translation.x, 0.0001f);
        assertEquals(0.0f, translation.y, 0.0001f);
        assertEquals(0.0f, translation.z, 0.0001f);
    }
    
    @Test
    void testPivotMath() {
        // Pivot at 8,8,8 (0.5, 0.5, 0.5). Rotate 90 deg Y.
        // T(pos=0) * T(piv) * R(90) * T(-piv)
        // Point (0,0,0) should rotate around (0.5, 0.5, 0.5)
        // Wait, the matrix applies to the CONTENT of the node (children/cubes).
        // If we have a cube at (0,0,0) inside this node.
        // GlobalPos = M * LocalPos
        // Let's check the matrix translation.
        // M = I * T(0.5) * R * T(-0.5)
        // This matrix represents the transform of the coordinate system.
        
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
        
        Model model = load(json);
        ModelInstance instance = model.instantiate();
        Matrix4f mat = instance.computeWorldTransforms().get(model.getRoots().get(0));
        
        // Let's verify by transforming a point.
        // The pivot is at center (0.5, 0.5, 0.5).
        // If we transform the pivot point itself, it should remain at (0.5, 0.5, 0.5)
        Vector3f point = new Vector3f(0.5f, 0.5f, 0.5f);
        mat.transformPosition(point);
        
        assertEquals(0.5f, point.x, 0.0001f);
        assertEquals(0.5f, point.y, 0.0001f);
        assertEquals(0.5f, point.z, 0.0001f);
    }
}
