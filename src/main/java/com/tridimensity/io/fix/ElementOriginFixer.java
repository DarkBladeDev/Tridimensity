package com.tridimensity.io.fix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.tridimensity.io.ast.ModelAst;

import java.util.*;

public class ElementOriginFixer implements ModelAutoFixer {
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
}
