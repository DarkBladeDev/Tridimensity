package com.tridimensity.io.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tridimensity.exception.ModelParseException;
import com.tridimensity.io.ast.ModelAst;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ElementRotationFixer implements ModelAutoFixer {
    @Override
    public void apply(ModelAst model, FixReport report) {
        for (JsonElement el : model.elements()) {
            JsonObject elem = el.getAsJsonObject();
            if (!ModelAst.hasNonZeroRotation(elem)) {
                continue;
            }

            String uuid = elem.has("uuid") ? elem.get("uuid").getAsString() : null;

            Vector3f pivot = readPivot(elem, model, uuid);

            float[] eRot = ModelAst.readRotation(elem);
            float rx = eRot[0];
            float ry = eRot[1];
            float rz = eRot[2];

            Vector3f from = readVec3(elem, "from");
            Vector3f to = readVec3(elem, "to");
            if (from == null || to == null) {
                int line = uuid != null ? model.lineOfUuid(uuid) : -1;
                throw new ModelParseException("Element missing 'from'/'to' for geometric rotation fix", line, uuid != null ? "/elements/" + uuid : null);
            }

            Vector3f[] verts = corners(from, to);
            Vector3f[] transformed = applyRotation(verts, pivot, rx, ry, rz);

            Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            for (Vector3f v : transformed) {
                min.x = Math.min(min.x, v.x);
                min.y = Math.min(min.y, v.y);
                min.z = Math.min(min.z, v.z);
                max.x = Math.max(max.x, v.x);
                max.y = Math.max(max.y, v.y);
                max.z = Math.max(max.z, v.z);
            }

            writeVec3(elem, "from", min);
            writeVec3(elem, "to", max);

            ModelAst.writeRotation(elem, new float[]{0f, 0f, 0f});

            if (elem.has("origin")) {
                elem.remove("origin");
            }

            report.warn(String.format(
                "Auto-fix: baked element rotation [%.2f, %.2f, %.2f] into geometry for '%s'",
                rx, ry, rz, model.elementName(elem)
            ));
        }
    }

    private static Vector3f readPivot(JsonObject elem, ModelAst model, String uuid) {
        if (elem.has("origin")) {
            JsonArray oa = elem.getAsJsonArray("origin");
            if (oa != null && oa.size() == 3) {
                return new Vector3f(oa.get(0).getAsFloat(), oa.get(1).getAsFloat(), oa.get(2).getAsFloat());
            }
        }
        JsonObject parent = uuid != null ? model.findParentGroupForElementUuid(uuid) : null;
        if (parent != null && parent.has("origin")) {
            JsonArray ga = parent.getAsJsonArray("origin");
            if (ga != null && ga.size() == 3) {
                return new Vector3f(ga.get(0).getAsFloat(), ga.get(1).getAsFloat(), ga.get(2).getAsFloat());
            }
        }
        int line = uuid != null ? model.lineOfUuid(uuid) : -1;
        throw new ModelParseException(
            "Element-level rotation requires geometric compensation and could not be fixed safely",
            line,
            uuid != null ? "/elements/" + uuid : null
        );
    }

    private static Vector3f readVec3(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonArray a = obj.getAsJsonArray(key);
        if (a == null || a.size() != 3) return null;
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static void writeVec3(JsonObject obj, String key, Vector3f v) {
        JsonArray a = new JsonArray();
        a.add(v.x);
        a.add(v.y);
        a.add(v.z);
        obj.add(key, a);
    }

    private static Vector3f[] corners(Vector3f from, Vector3f to) {
        float fx = from.x, fy = from.y, fz = from.z;
        float tx = to.x, ty = to.y, tz = to.z;
        return new Vector3f[] {
            new Vector3f(fx, fy, fz), new Vector3f(tx, fy, fz), new Vector3f(fx, ty, fz), new Vector3f(tx, ty, fz),
            new Vector3f(fx, fy, tz), new Vector3f(tx, fy, tz), new Vector3f(fx, ty, tz), new Vector3f(tx, ty, tz)
        };
    }

    private static Vector3f[] applyRotation(Vector3f[] verts, Vector3f pivot, float rxDeg, float ryDeg, float rzDeg) {
        Matrix4f rot = new Matrix4f().identity()
            .rotateXYZ((float) Math.toRadians(rxDeg), (float) Math.toRadians(ryDeg), (float) Math.toRadians(rzDeg));
        Vector3f[] out = new Vector3f[verts.length];
        for (int i = 0; i < verts.length; i++) {
            Vector3f v = new Vector3f(verts[i]).sub(pivot);
            rot.transformPosition(v);
            v.add(pivot);
            out[i] = v;
        }
        return out;
    }
}
