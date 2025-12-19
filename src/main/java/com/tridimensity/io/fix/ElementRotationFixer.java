package com.tridimensity.io.fix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tridimensity.exception.ModelParseException;
import com.tridimensity.io.ast.ModelAst;
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

            float[] eRot = ModelAst.readRotation(elem);
            float rx = eRot[0];
            float ry = eRot[1];
            float rz = eRot[2];

            Vector3f pivot = readPivot(elem, model, uuid);

            // Build wrapper group that will hold the rotation and pivot
            JsonObject wrapper = new JsonObject();
            String name = model.elementName(elem);
            wrapper.addProperty("name", name);

            JsonArray originArr = new JsonArray();
            originArr.add(pivot.x);
            originArr.add(pivot.y);
            originArr.add(pivot.z);
            wrapper.add("origin", originArr);

            JsonArray rotArr = new JsonArray();
            rotArr.add(rx);
            rotArr.add(ry);
            rotArr.add(rz);
            wrapper.add("rotation", rotArr);

            JsonArray childrenArr = new JsonArray();
            childrenArr.add(uuid);
            wrapper.add("children", childrenArr);

            // Insert wrapper replacing the element UUID in its parent children list
            boolean replaced = replaceElementReferenceWithGroup(model, uuid, wrapper);
            if (!replaced) {
                int line = uuid != null ? model.lineOfUuid(uuid) : -1;
                throw new ModelParseException(
                    "Element-level rotation requires a parent group to wrap the element",
                    line,
                    uuid != null ? "/elements/" + uuid : null
                );
            }

            // Clear element-level rotation and origin
            ModelAst.writeRotation(elem, new float[]{0f, 0f, 0f});
            if (elem.has("origin")) {
                elem.remove("origin");
            }

            report.warn(String.format(
                "Auto-fix: moved element rotation [%.2f, %.2f, %.2f] to a wrapper group for '%s'",
                rx, ry, rz, model.elementName(elem)
            ));
        }
    }

    private static Vector3f readPivot(JsonObject elem, ModelAst model, String uuid) {
        // Try element's own origin first
        if (elem.has("origin")) {
            JsonArray oa = elem.getAsJsonArray("origin");
            if (oa != null && oa.size() == 3) {
                return new Vector3f(
                    oa.get(0).getAsFloat(), 
                    oa.get(1).getAsFloat(), 
                    oa.get(2).getAsFloat()
                );
            }
        }
        
        // Fallback to parent group's origin
        JsonObject parent = uuid != null ? model.findParentGroupForElementUuid(uuid) : null;
        if (parent != null && parent.has("origin")) {
            JsonArray ga = parent.getAsJsonArray("origin");
            if (ga != null && ga.size() == 3) {
                return new Vector3f(
                    ga.get(0).getAsFloat(), 
                    ga.get(1).getAsFloat(), 
                    ga.get(2).getAsFloat()
                );
            }
        }
        
        // No valid pivot found
        int line = uuid != null ? model.lineOfUuid(uuid) : -1;
        throw new ModelParseException(
            "Element-level rotation requires a pivot (origin) and could not be determined",
            line,
            uuid != null ? "/elements/" + uuid : null
        );
    }

    private static boolean replaceElementReferenceWithGroup(ModelAst model, String uuid, JsonObject wrapperGroup) {
        JsonArray outliner = model.outliner();
        for (int i = 0; i < outliner.size(); i++) {
            JsonElement node = outliner.get(i);
            if (node.isJsonPrimitive() && node.getAsJsonPrimitive().isString()) {
                if (uuid.equals(node.getAsString())) {
                    outliner.set(i, wrapperGroup);
                    return true;
                }
            } else if (node.isJsonObject()) {
                JsonObject group = node.getAsJsonObject();
                if (!group.has("children")) continue;
                JsonArray children = group.getAsJsonArray("children");
                for (int j = 0; j < children.size(); j++) {
                    JsonElement child = children.get(j);
                    if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                        if (uuid.equals(child.getAsString())) {
                            children.set(j, wrapperGroup);
                            return true;
                        }
                    } else if (child.isJsonObject()) {
                        // Nested groups: continue search in depth
                        // We avoid deep recursion to keep it simple and efficient
                        JsonObject nested = child.getAsJsonObject();
                        if (!nested.has("children")) continue;
                        JsonArray nestedChildren = nested.getAsJsonArray("children");
                        for (int k = 0; k < nestedChildren.size(); k++) {
                            JsonElement nc = nestedChildren.get(k);
                            if (nc.isJsonPrimitive() && nc.getAsJsonPrimitive().isString() && uuid.equals(nc.getAsString())) {
                                nestedChildren.set(k, wrapperGroup);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
