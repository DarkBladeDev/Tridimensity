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
    void testOutlinerMissingOriginPasses() {
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
        Model model = load(json);
        assertNotNull(model);
        assertEquals(1, model.getRoots().size());
        ModelNode root = model.getRoots().get(0);
        // Default origin should be (0,0,0) when missing in outliner
        assertEquals(new Vector3f(0,0,0), root.getOrigin());
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
    void testElementRotationFails() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "11111111-1111-1111-1111-111111111111",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "rotation": [0, 90, 0]
                    }
                ],
                "outliner": [
                    {
                        "name": "root",
                        "origin": [0,0,0],
                        "children": ["11111111-1111-1111-1111-111111111111"]
                    }
                ]
            }
            """;
        assertThrows(ModelParseException.class, () -> load(json));
    }

    @Test
    void testElementOriginFails() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "22222222-2222-2222-2222-222222222222",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "origin": [8, 8, 8]
                    }
                ],
                "outliner": [
                    {
                        "name": "root",
                        "origin": [0,0,0],
                        "children": ["22222222-2222-2222-2222-222222222222"]
                    }
                ]
            }
            """;
        // En modo auto-fix, un origin sin rotación se limpia
        var options = new com.tridimensity.io.options.ParserOptions(true);
        Model model = BlockbenchLoader.load(new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)), options);
        assertNotNull(model);
    }

    @Test
    void testElementRotationAutoFix() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "name": "box",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "rotation": [0, 90, 0]
                    }
                ],
                "outliner": [
                    {
                        "name": "group",
                        "origin": [0,0,0],
                        "rotation": [0, 45, 0],
                        "children": ["aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"]
                    }
                ]
            }
            """;

        var options = new com.tridimensity.io.options.ParserOptions(true);
        Model model = BlockbenchLoader.load(new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)), options);
        assertNotNull(model);
        assertEquals(1, model.getRoots().size());
        ModelNode group = model.getRoots().get(0);
        // Con auto-fix geométrico, no se mueve la rotación al grupo
        assertEquals(45.0f, group.getRotation().y, 0.0001f);
        // group.origin should equal element origin
        assertEquals(0.0f, group.getOrigin().x, 0.0001f);
        assertEquals(0.0f, group.getOrigin().y, 0.0001f);
        assertEquals(0.0f, group.getOrigin().z, 0.0001f);
    }

    @Test
    void testElementRotationNoParentFails() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                        "name": "lonely",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "rotation": [0, 90, 0]
                    }
                ],
                "outliner": []
            }
            """;

        var options = new com.tridimensity.io.options.ParserOptions(true);
        assertThrows(ModelParseException.class, () -> BlockbenchLoader.load(new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)), options));
    }

    @Test
    void testElementOriginAutoFixNoConflict() {
        String json = """
            {
                "elements": [
                    {
                        "uuid": "c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1",
                        "name": "a",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "rotation": [0, 90, 0],
                        "origin": [0, 0, 0]
                    },
                    {
                        "uuid": "d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2",
                        "name": "b",
                        "from": [0,0,0],
                        "to": [16,16,16],
                        "faces": {},
                        "rotation": [0, 45, 0],
                        "origin": [1, 0, 0]
                    }
                ],
                "outliner": [
                    {
                        "name": "group",
                        "origin": [0,0,0],
                        "children": [
                            "c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1",
                            "d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2"
                        ]
                    }
                ]
            }
            """;

        var options = new com.tridimensity.io.options.ParserOptions(true);
        Model model = BlockbenchLoader.load(new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)), options);
        assertNotNull(model);
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
        // Geometry at 0,0,0 (global).
        // Bone Matrix should be at 0.5, 0.5, 0.5.
        // Render Matrix should effectively rotate geometry around 0.5, 0.5, 0.5.
        
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
        
        // Test Point: Pivot (0.5, 0.5, 0.5).
        // Since we rotate around the pivot, the pivot point itself should NOT move in world space.
        // M_render = M_bone * T(-Pivot)
        // P_world = M_render * P_global
        // P_global = Pivot (0.5, 0.5, 0.5)
        // P_local = T(-Pivot) * Pivot = 0,0,0
        // P_final = M_bone * 0 = BonePosition.
        // BonePosition for root = T(Pivot - 0) = T(Pivot).
        // So P_final = Pivot. Correct.
        
        Vector3f point = new Vector3f(0.5f, 0.5f, 0.5f);
        mat.transformPosition(point);
        
        assertEquals(1.0f, point.x, 0.0001f);
        assertEquals(1.0f, point.y, 0.0001f);
        assertEquals(1.0f, point.z, 0.0001f);
        
        // Test Point: (0,0,0) corner.
        // Relative to pivot (0.5, 0.5, 0.5), it is at (-0.5, -0.5, -0.5).
        // Rotate 90 Y -> (-0.5, -0.5, 0.5) approx. (X becomes Z, Z becomes -X... wait. Rot Y: X->Z, Z->-X? No. Z->X, X->-Z?)
        // Let's use JOML logic.
        // Rot Y 90: (+X -> -Z).
        // Point relative: (-0.5, -0.5, -0.5).
        // Rotated: (-0.5, -0.5, 0.5).
        // Add Pivot back: (0, 0, 1).
        
        Vector3f corner = new Vector3f(0.0f, 0.0f, 0.0f);
        mat.transformPosition(corner);
        
        assertEquals(1.5f, corner.x, 0.0001f);
        assertEquals(0.5f, corner.y, 0.0001f);
        assertEquals(0.5f, corner.z, 0.0001f);
    }

    @Test
    void testLoadFromFile() throws Exception {
        try (var stream = getClass().getResourceAsStream("/models/default.bbmodel")) {
            assertNotNull(stream, "Test resource " + "/models/default.bbmodel" + " not found");
            assertThrows(ModelParseException.class, () -> BlockbenchLoader.load(stream));
        }
    }
}
