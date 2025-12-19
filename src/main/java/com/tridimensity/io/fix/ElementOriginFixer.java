package com.tridimensity.io.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
                // Do not move origin to group; leave it for ElementRotationFixer to consume/remove
            }
        }

        // No group modifications
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
