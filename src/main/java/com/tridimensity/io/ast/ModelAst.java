package com.tridimensity.io.ast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tridimensity.exception.ModelParseException;

public class ModelAst {
    private final JsonObject root;
    private final String raw;

    public ModelAst(JsonObject root, String raw) {
        this.root = root;
        this.raw = raw;
    }

    public static ModelAst fromJson(JsonObject root, String raw) {
        return new ModelAst(root, raw);
    }

    public JsonArray elements() {
        return root.getAsJsonArray("elements");
    }

    public JsonArray outliner() {
        return root.getAsJsonArray("outliner");
    }

    public static boolean hasNonZeroRotation(JsonObject obj) {
        if (!obj.has("rotation")) return false;
        JsonArray arr = obj.getAsJsonArray("rotation");
        if (arr == null || arr.size() != 3) return false;
        float x = arr.get(0).getAsFloat();
        float y = arr.get(1).getAsFloat();
        float z = arr.get(2).getAsFloat();
        return Math.abs(x) > 1e-6f || Math.abs(y) > 1e-6f || Math.abs(z) > 1e-6f;
    }

    public static float[] readRotation(JsonObject obj) {
        float[] rot = new float[] {0f, 0f, 0f};
        if (obj.has("rotation")) {
            JsonArray arr = obj.getAsJsonArray("rotation");
            if (arr != null && arr.size() == 3) {
                rot[0] = arr.get(0).getAsFloat();
                rot[1] = arr.get(1).getAsFloat();
                rot[2] = arr.get(2).getAsFloat();
            }
        }
        return rot;
    }

    public static void writeRotation(JsonObject obj, float[] rot) {
        JsonArray arr = new JsonArray();
        arr.add(rot[0]);
        arr.add(rot[1]);
        arr.add(rot[2]);
        obj.add("rotation", arr);
    }

    public int lineOfUuid(String uuid) {
        if (raw == null || uuid == null) return -1;
        String needle = uuid;
        int idx = raw.indexOf(needle);
        if (idx < 0) return -1;
        int line = 1;
        for (int i = 0; i < idx; i++) {
            if (raw.charAt(i) == '\n') line++;
        }
        return line;
    }

    public int lineOfGroupName(String name) {
        if (raw == null || name == null) return -1;
        int idx = raw.indexOf(name);
        if (idx < 0) return -1;
        int line = 1;
        for (int i = 0; i < idx; i++) {
            if (raw.charAt(i) == '\n') line++;
        }
        return line;
    }

    public JsonObject findParentGroupForElementUuid(String uuid) {
        for (JsonElement nodeEl : outliner()) {
            JsonObject found = findParentInGroup(nodeEl.getAsJsonObject(), uuid);
            if (found != null) return found;
        }
        return null;
    }

    private JsonObject findParentInGroup(JsonObject group, String uuid) {
        if (!group.has("children")) return null;
        JsonArray children = group.getAsJsonArray("children");
        for (JsonElement child : children) {
            if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                String id = child.getAsString();
                if (uuid.equals(id)) return group;
            } else if (child.isJsonObject()) {
                JsonObject nested = child.getAsJsonObject();
                JsonObject found = findParentInGroup(nested, uuid);
                if (found != null) return found;
            }
        }
        return null;
    }

    public void validateNoElementRotations() {
        for (JsonElement el : elements()) {
            JsonObject obj = el.getAsJsonObject();
            if (hasNonZeroRotation(obj)) {
                throw new ModelParseException("Element-level transforms are not supported; auto-fix failed");
            }
        }
    }

    public String elementName(JsonObject el) {
        return el.has("name") ? el.get("name").getAsString() : "<unknown>";
    }

    public String groupName(JsonObject group) {
        return group.has("name") ? group.get("name").getAsString() : "<group>";
    }
}
