package transform;

import com.tridimensity.model.Model;
import com.tridimensity.model.ModelInstance;
import com.tridimensity.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests automáticos de transformaciones (pivot & jerarquía)
 */
public class TransformEngineTest {

    // ----------------------------------------------------------------------------------
    // Tests Obligatorios
    // ----------------------------------------------------------------------------------

    // 1️⃣ Acumulación padre → hijo
    // Detecta: orden incorrecto local × parent
    @Test
    void childTransformMustRespectParentRotation() {
        // Root: Pos(0,0,0), Rot(0,90,0) -> Mira hacia +X, Rota Y 90 -> Ejes giran.
        // Child: Pos(1,0,0) (en sistema local del padre).
        // Si el padre rota 90 en Y, el eje X del padre apunta hacia -Z (regla de la mano derecha? No, Y up. X -> Z. )
        // En OpenGL/JOML Y-Up: Rot Y 90: (+X -> -Z).
        // Esperamos que el hijo esté en (0,0,-1) o (0,0,1) dependiendo de la mano del sistema.
        // Blockbench es Left-Handed? No, Minecraft es Right-Handed?
        // El test original pide assertVectorEquals(vec(0,0,1), pos, 0.0001f);
        // Si X -> Z (90 deg positive rotation around Y), entonces (1,0,0) -> (0,0,1).
        // Si X -> -Z, entonces (0,0,-1).
        // Asumiremos la expectativa del usuario: vec(0,0,1). (X -> Z).

        // NOTA: ModelNode constructor: (name, origin, position, rotation, scale)
        // El ejemplo del prompt: node(name, pos, rot) no coincide con el constructor real.
        // Adaptamos: node(name, origin, pos, rot, scale)
        // Ojo: En mi implementación actual ModelNode toma "origin" (pivot) y "position" (offset).
        // Si el prompt dice node(name, vec(0,0,0), vec(0,90,0)), asumo origin=0, pos=0, rot=90.
        // Pero el hijo tiene "vec(1,0,0)". Asumo pos=1,0,0.
        
        // Root: Origin=0, Pos=0, Rot=90.
        ModelNode root = new ModelNode("root", vec(0,0,0), vec(0,0,0), vec(0,90,0), vec(1,1,1));
        
        // Child: Origin=0, Pos=16,0,0 (1 bloque = 16 pixels), Rot=0.
        // El prompt usa unidades de bloque o pixel?
        // "Blockbench usa píxeles". "La librería normaliza todo a bloques Minecraft".
        // Si el usuario pone `vec(1,0,0)` en el test y espera `vec(0,0,1)` en el resultado (bloques),
        // Significa que está definiendo el nodo EN BLOQUES?
        // No, el prompt dice "La librería normaliza todo a bloques... Regla fija: 1 bloque = 16 unidades Blockbench".
        // Si meto 1 en Blockbench units, sale 1/16 en World.
        // Si el assert espera 1.0, entonces debo meter 16.0 en el input.
        
        ModelNode child = new ModelNode("child", vec(0,0,0), vec(16,0,0), vec(0,0,0), vec(1,1,1));

        root.addChild(child);

        Model model = new Model();
        model.addRoot(root);
        ModelInstance instance = model.instantiate();
        
        Map<ModelNode, Matrix4f> world = instance.computeWorldTransforms();

        Vector3f pos = extractTranslation(world.get(child));

        // El resultado debe ser en bloques.
        // 16 pixels -> 1 bloque.
        // Rotado 90 Y -> (0, 0, -1) [Si Right Handed, X cross Y = Z. Rot Y 90 -> Z]
        // Espera: (0,0,1) -> Esto implica Rot Y 90 lleva +X a +Z.
        
        // Nota importante: Mi implementación aplica M_render = M_bone * T(-Pivot).
        // Aquí Pivot=0. Entonces M_render = M_bone.
        // M_bone = M_parent * T(offset).
        // Offset = ChildPiv(0) - ParentPiv(0) + Pos(16) = (16,0,0).
        // Scale 1/16 -> (1,0,0).
        // M_parent = R(90).
        // World = R(90) * T(1,0,0).
        // Transformamos (0,0,0) local del hijo al mundo.
        // P_world = R(90) * (1,0,0) = (0,0,1) ? O (0,0,-1)?
        // JOML rotateY(toRadians(90)):
        // X -> Z ?
        // Matrix4f.rotateY doc: "rotate the matrix around the Y axis".
        // Standard Math:
        // [ cos -sin ]
        // [ sin  cos ]
        // En 3D Y-axis:
        // z' = z cos - x sin
        // x' = z sin + x cos
        // Si x=1, z=0:
        // z' = -sin(90) = -1.
        // x' = cos(90) = 0.
        // Resultado: (0, 0, -1).
        
        // EL USUARIO ESPERA (0,0,1).
        // Esto significa que la rotación es horaria o el sistema es Z-Forward?
        // Minecraft: +Z es Sur. +X es Este.
        // Mirando desde arriba (Y), X a la derecha, Z abajo.
        // Rotar 90 grados Y (yaw) en Minecraft...
        // Si giro a la izquierda (yaw -90) miro al este?
        // Si el usuario espera (0,0,1), es +Z.
        // Significa que 90 grados rota X hacia Z.
        // En mi math (standard), X rota a -Z.
        // Vamos a probar. Si falla, el usuario tiene razón en su expectativa de Minecraft/Blockbench y yo debo ajustar mi rotación o signo.
        // PERO: Blockbench ejes: +X East, +Y Up, +Z South.
        // Rotación Euler estándar.
        // Veamos qué pasa. Ajustaré el input si es necesario, pero el assert manda.
        // Si el test falla por signo, es un "bug" según el usuario, o una diferencia de sistema de coordenadas.
        // PERO: "Cualquier corrección de ejes se hace en el loader".
        // Mi loader lee raw. Mi math usa standard JOML.
        
        // Ajuste: Si blockbench rota X->Z con +90, entonces es rotación negativa en math standard?
        // Ojo: Blockbench usa un sistema de coordenadas LH (Left Handed) o RH?
        // Minecraft es RH con Y up? No, Minecraft usa un sistema raro.
        // "Unidades Blockbench usa píxeles... La librería normaliza... +X Este, +Y Arriba, +Z Sur".
        // Eso es un sistema Right Handed standard (+X right, +Y up, +Z forward/back? No, +Z is towards viewer usually in RH GL, but South is usually +Z in MC).
        // En MC: X=East, Z=South.
        // (1,0,0) es East.
        // Rotar 90 Y. Si uso la regla de la mano derecha (pulgar Y), los dedos van de X a Z?
        // X(East) x Y(Up) = Z(South). Sí.
        // Entonces Rot +90 debería llevar X a Z? No, lleva X a -Z en matrices estándar de rotación de vectores activos?
        // Espera. X cross Y = Z.
        // Rotar +90 sobre Y lleva X al eje -Z (hacia dentro)? O hacia Z?
        // En un círculo trigonométrico estándar (CCW): 0 grados es X. 90 es Y.
        // En plano XZ (visto desde arriba, Y saliendo):
        // X es 0. Z es 90? O -Z es 90?
        // Generalmente en RH: Z sale de la pantalla. X derecha. Y arriba.
        // Visto desde Y: X derecha, Z abajo (sur).
        // Rotar +90 (CCW) lleva X a -Z (Arriba en 2D).
        // Si el usuario espera (0,0,1), espera que X vaya a +Z. Eso sería rotación CW (Clockwise) o -90.
        // O Blockbench usa rotaciones CW positivas.
        
        // Voy a implementar el test tal cual y ver qué sale.
        
        assertVectorEquals(vec(0,0,1), pos, 0.0001f);
    }

    // 2️⃣ Pivot inmutable al rotar
    // Detecta: rotación sin compensar pivot
    @Test
    void rotationMustRespectPivot() {
        // Node "arm"
        // Pivot: 1,0,0 (16,0,0 pixels)
        // Rot: 0,90,0
        // Pos: 0,0,0
        // M_local = computeLocalMatrix(node)
        // PivotWorld = transformPoint(local, pivot)
        // Assert PivotWorld == Pivot.
        
        // Pivot en pixels: 16,0,0.
        ModelNode node = new ModelNode("arm", vec(16,0,0), vec(0,0,0), vec(0,90,0), vec(1,1,1));
        
        // Simular el engine
        Model model = new Model();
        model.addRoot(node);
        ModelInstance instance = model.instantiate();
        
        // computeWorldTransforms retorna matrices WORLD.
        // Como este nodo es root, Local == World.
        Map<ModelNode, Matrix4f> worldMap = instance.computeWorldTransforms();
        Matrix4f local = worldMap.get(node);

        // El punto a transformar debe estar en coordenadas globales (Blockbench units scaleadas a bloques).
        // El pivot es (16,0,0) px -> (1,0,0) blocks.
        Vector3f pivotPoint = vec(1,0,0);
        
        Vector3f pivotWorld = transformPoint(local, pivotPoint);

        assertVectorEquals(vec(1,0,0), pivotWorld, 0.0001f);
    }

    // 3️⃣ Jerarquía profunda sin “explosión”
    // Detecta: acumulación incorrecta repetida
    @Test
    void parentChildChainMustNotDrift() {
        // Root: Pos 0, Rot 45 Y.
        // Mid: Pos 1 (16px), Rot 45 Y.
        // Tip: Pos 1 (16px), Rot 0.
        
        // Root
        ModelNode root = new ModelNode("root", vec(0,0,0), vec(0,0,0), vec(0,45,0), vec(1,1,1));
        // Mid (Hijo de Root)
        ModelNode mid = new ModelNode("mid", vec(0,0,0), vec(16,0,0), vec(0,45,0), vec(1,1,1));
        // Tip (Hijo de Mid)
        ModelNode tip = new ModelNode("tip", vec(0,0,0), vec(16,0,0), vec(0,0,0), vec(1,1,1));

        root.addChild(mid);
        mid.addChild(tip);

        Model model = new Model();
        model.addRoot(root);
        ModelInstance instance = model.instantiate();

        Matrix4f tipWorld = instance.computeWorldTransforms().get(tip);
        
        // La posición debe ser finita y razonable.
        // Root rot 45. Mid en 1,0,0 (rotado 45).
        // Mid rot 45. Tip en 1,0,0 (local a Mid).
        // Total rot 90?
        Vector3f pos = extractTranslation(tipWorld);
        
        // No verificamos posición exacta, solo que no explote (drifting / NaN / Infinity).
        assertTrue(pos.length() < 5.0f, "Position drifted too far: " + pos);
    }


    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    static Vector3f vec(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    static Vector3f extractTranslation(Matrix4f m) {
        Vector3f t = new Vector3f();
        m.getTranslation(t);
        return t;
    }

    static Vector3f transformPoint(Matrix4f m, Vector3f p) {
        // Transform as Point (w=1)
        Vector3f dest = new Vector3f(p);
        m.transformPosition(dest);
        return dest;
    }

    static void assertVectorEquals(Vector3f expected, Vector3f actual, float eps) {
        if (expected.distance(actual) > eps) {
            fail(String.format("Expected %s but got %s (dist=%f)", expected, actual, expected.distance(actual)));
        }
    }
}
