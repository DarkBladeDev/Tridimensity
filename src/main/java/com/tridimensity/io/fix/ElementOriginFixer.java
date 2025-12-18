package com.tridimensity.io.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tridimensity.exception.ModelParseException;
import com.tridimensity.io.ast.ModelAst;

import java.util.*;

public class ElementOriginFixer implements ModelAutoFixer {
    private static final float EPS = 1e-6f;

    @Override
    public void apply(ModelAst model, FixReport report) {
        Map<JsonObject, List<JsonObject>> rotatedByGroup = new HashMap<>();

        for (JsonElement el : model.elements()) {
            JsonObject elem = el.getAsJsonObject();
            boolean hasOrigin = elem.has("origin");
            boolean hasRot = ModelAst.hasNonZeroRotation(elem);

            if (hasOrigin && !hasRot) {
                elem.remove("origin");
                report.warn(String.format("Auto-fix: removed element origin from '%s' (rotation is zero)", model.elementName(elem)));
                continue;
            }

            if (hasOrigin && hasRot) {
                String uuid = elem.has("uuid") ? elem.get("uuid").getAsString() : null;
                JsonObject parent = uuid != null ? model.findParentGroupForElementUuid(uuid) : null;
                if (parent == null) {
                    int line = uuid != null ? model.lineOfUuid(uuid) : -1;
                    throw new ModelParseException("Element-level origin found but element has no parent group: " + model.elementName(elem), line, uuid != null ? "/elements/" + uuid : null);
                }
                rotatedByGroup.computeIfAbsent(parent, k -> new ArrayList<>()).add(elem);
            }
        }

        for (Map.Entry<JsonObject, List<JsonObject>> entry : rotatedByGroup.entrySet()) {
            JsonObject group = entry.getKey();
            List<JsonObject> elems = entry.getValue();

            float[] unique = null;
            for (JsonObject e : elems) {
                float[] o = readOrigin(e);
                if (unique == null) {
                    unique = o;
                } else if (!approxEquals(unique, o)) {
                    int line = model.lineOfGroupName(model.groupName(group));
                    throw new ModelParseException("Auto-fix cannot resolve multiple element origins in group '" + model.groupName(group) + "'", line, "/outliner/" + model.groupName(group));
                }
            }

            if (unique != null) {
                writeOrigin(group, unique);
                for (JsonObject e : elems) {
                    e.remove("origin");
                    report.warn(String.format(
                        "Auto-fix: moved origin [%.2f, %.2f, %.2f] from element '%s' to group '%s'",
                        unique[0], unique[1], unique[2],
                        model.elementName(e), model.groupName(group)
                    ));
                }
            }
        }
    }

    private static float[] readOrigin(JsonObject obj) {
        JsonArray arr = obj.getAsJsonArray("origin");
        return new float[] {
            arr.get(0).getAsFloat(),
            arr.get(1).getAsFloat(),
            arr.get(2).getAsFloat()
        };
    }

    private static void writeOrigin(JsonObject obj, float[] o) {
        JsonArray arr = new JsonArray();
        arr.add(o[0]);
        arr.add(o[1]);
        arr.add(o[2]);
        obj.add("origin", arr);
    }

    private static boolean approxEquals(float[] a, float[] b) {
        return Math.abs(a[0]-b[0]) < EPS && Math.abs(a[1]-b[1]) < EPS && Math.abs(a[2]-b[2]) < EPS;
    }
}
