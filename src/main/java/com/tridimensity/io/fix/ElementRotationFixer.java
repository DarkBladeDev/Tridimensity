package com.tridimensity.io.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tridimensity.exception.ModelParseException;
import com.tridimensity.io.ast.ModelAst;

public class ElementRotationFixer implements ModelAutoFixer {
    @Override
    public void apply(ModelAst model, FixReport report) {
        for (JsonElement el : model.elements()) {
            JsonObject elem = el.getAsJsonObject();
            if (ModelAst.hasNonZeroRotation(elem)) {
                String uuid = elem.has("uuid") ? elem.get("uuid").getAsString() : null;
                JsonObject parent = uuid != null ? model.findParentGroupForElementUuid(uuid) : null;
                if (parent == null) {
                    String name = model.elementName(elem);
                    int line = uuid != null ? model.lineOfUuid(uuid) : -1;
                    throw new ModelParseException("Element-level rotation found but element has no parent group: " + name, line, uuid != null ? "/elements/" + uuid : null);
                }

                float[] eRot = ModelAst.readRotation(elem);
                float[] gRot = ModelAst.readRotation(parent);
                gRot[0] += eRot[0];
                gRot[1] += eRot[1];
                gRot[2] += eRot[2];
                ModelAst.writeRotation(parent, gRot);

                float[] zero = new float[] {0f, 0f, 0f};
                ModelAst.writeRotation(elem, zero);

                String msg = String.format(
                    "Auto-fix: moved rotation [%.2f, %.2f, %.2f] from element '%s' to group '%s'",
                    eRot[0], eRot[1], eRot[2],
                    model.elementName(elem), model.groupName(parent)
                );
                report.warn(msg);
            }
        }
    }
}
