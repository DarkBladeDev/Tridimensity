package com.tridimensity.io;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tridimensity.exception.ModelParseException;
import com.tridimensity.io.ast.ModelAst;
import com.tridimensity.io.dto.ElementDto;
import com.tridimensity.io.fix.ElementRotationFixer;
import com.tridimensity.io.fix.FixReport;
import com.tridimensity.io.options.ParserOptions;
import com.tridimensity.model.Model;
import com.tridimensity.model.ModelCube;
import com.tridimensity.model.ModelFace;
import com.tridimensity.model.ModelNode;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockbenchLoader {

    private static final Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(BlockbenchLoader.class);

    /**
     * Loads a Blockbench model from an InputStream.
     *
     * @param inputStream The JSON input stream.
     * @return The parsed Model.
     * @throws ModelParseException If the model is invalid.
     */
    public static Model load(InputStream inputStream) {
        return load(inputStream, ParserOptions.strict());
    }

    public static Model load(InputStream inputStream, ParserOptions options) {
        try {
            String raw = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            prepare(root, options, raw);
            return parse(root, raw);
        } catch (Exception e) {
            if (e instanceof ModelParseException) {
                throw (ModelParseException) e;
            }
            throw new ModelParseException("Failed to parse JSON", e);
        }
    }

    private static void prepare(JsonObject root, ParserOptions options, String raw) {
        if (options != null && options.isAutoFixTransforms()) {
            FixReport report = new FixReport();
            ModelAst ast = ModelAst.fromJson(root, raw);
            new com.tridimensity.io.fix.ElementOriginFixer().apply(ast, report);
            new ElementRotationFixer().apply(ast, report);
            ast.validateNoElementRotations();
            for (String w : report.warnings()) {
                log.warn(w);
            }
        }
    }

    private static Model parse(JsonObject root, String raw) {
        ModelAst ast = ModelAst.fromJson(root, raw);
        
        // 1. Parse Elements (Cubes)
        if (!root.has("elements")) {
            throw new ModelParseException("Missing 'elements' array", ast.lineOfKey("elements"), "/elements");
        }
        
        Map<UUID, ModelCube> elementMap = new HashMap<>();
        JsonArray elementsArray = root.getAsJsonArray("elements");
        
        for (JsonElement el : elementsArray) {
            ElementDto dto = gson.fromJson(el, ElementDto.class);
            JsonObject elObj = el.getAsJsonObject();
            
            if (elObj.has("origin")) {
                int line = dto.uuid != null ? ast.lineOfUuid(dto.uuid) : -1;
                throw new ModelParseException("Element-level origin is not supported; use group origin", line, dto.uuid != null ? "/elements/" + dto.uuid : null);
            }
            
            if (elObj.has("rotation")) {
                JsonArray ra = elObj.getAsJsonArray("rotation");
                if (ra != null && ra.size() == 3) {
                    float rx = ra.get(0).getAsFloat();
                    float ry = ra.get(1).getAsFloat();
                    float rz = ra.get(2).getAsFloat();
                    if (Math.abs(rx) > 1e-6f || Math.abs(ry) > 1e-6f || Math.abs(rz) > 1e-6f) {
                        int line = dto.uuid != null ? ast.lineOfUuid(dto.uuid) : -1;
                        throw new ModelParseException("Element-level transforms are not supported; use group rotation", line, dto.uuid != null ? "/elements/" + dto.uuid : null);
                    }
                }
            }
            
            validateElement(dto, ast);
            
            UUID uuid = UUID.fromString(dto.uuid);
            if (elementMap.containsKey(uuid)) {
                int line = ast.lineOfUuid(uuid.toString());
                throw new ModelParseException("Duplicate element UUID: " + uuid, line, "/elements/" + uuid);
            }
            
            Vector3f from = new Vector3f(dto.from[0], dto.from[1], dto.from[2]);
            Vector3f to = new Vector3f(dto.to[0], dto.to[1], dto.to[2]);
            
            Map<String, ModelFace> faces = new HashMap<>();
            if (dto.faces != null) {
                for (Map.Entry<String, ElementDto.FaceDto> entry : dto.faces.entrySet()) {
                    ElementDto.FaceDto f = entry.getValue();
                    String texture = f.texture != null ? f.texture : "";
                    faces.put(entry.getKey(), new ModelFace(f.uv, texture, f.rotation));
                }
            }
            
            ModelCube cube = new ModelCube(
                uuid,
                dto.name,
                from,
                to,
                faces
            );
            elementMap.put(uuid, cube);
        }

        // 2. Parse Outliner (Hierarchy)
        if (!root.has("outliner")) {
            throw new ModelParseException("Missing 'outliner' array", ast.lineOfKey("outliner"), "/outliner");
        }

        Model model = new Model();
        JsonArray outlinerArray = root.getAsJsonArray("outliner");

        Map<String, JsonObject> groupMap = new HashMap<>();
        if (root.has("groups")) {
            JsonArray groupsArray = root.getAsJsonArray("groups");
            for (JsonElement ge : groupsArray) {
                JsonObject g = ge.getAsJsonObject();
                if (g.has("uuid")) {
                    groupMap.put(g.get("uuid").getAsString(), g);
                }
            }
        }

        Set<UUID> usedCubes = new HashSet<>();

        for (JsonElement nodeJson : outlinerArray) {
            if (nodeJson.isJsonObject()) {
                ModelNode node = parseNode(nodeJson.getAsJsonObject(), elementMap, groupMap, usedCubes, ast);
                model.addRoot(node);
            } else if (nodeJson.isJsonPrimitive() && nodeJson.getAsJsonPrimitive().isString()) {
                // Allow root entries that are direct element UUIDs
                String uuidStr = nodeJson.getAsString();
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    int line = ast.lineOfUuid(uuidStr);
                    throw new ModelParseException("Invalid UUID format: " + uuidStr, line, "/outliner/" + uuidStr);
                }

                if (!elementMap.containsKey(uuid)) {
                    int line = ast.lineOfUuid(uuidStr);
                    throw new ModelParseException("Reference to nonexistent cube UUID: " + uuid, line, "/outliner/" + uuidStr);
                }

                ModelNode synthetic = new ModelNode(
                    "<outliner-root>", 
                    new Vector3f(0, 0, 0),
                    new Vector3f(0, 0, 0), 
                    new Vector3f(0, 0, 0), 
                    new Vector3f(1, 1, 1)
                );
                synthetic.addCube(elementMap.get(uuid));
                
                if (usedCubes.contains(uuid)) {
                    int line = ast.lineOfUuid(uuidStr);
                    throw new ModelParseException("Cube referenced more than once: " + uuid, line, "/outliner/" + uuidStr);
                }
                usedCubes.add(uuid);
                model.addRoot(synthetic);
            } else {
                throw new ModelParseException("Unknown root entry type in outliner: " + nodeJson, ast.lineOfKey("outliner"), "/outliner");
            }
        }

        if (model.getRoots().isEmpty()) {
            throw new ModelParseException("Model has no root nodes", ast.lineOfKey("outliner"), "/outliner");
        }
        
        return model;
    }

    private static void validateElement(ElementDto dto, ModelAst ast) {
        if (dto.uuid == null) {
            throw new ModelParseException("Element missing UUID", ast.lineOfKey("elements"), "/elements");
        }
        if (dto.from == null || dto.from.length != 3) {
            int line = dto.uuid != null ? ast.lineOfUuid(dto.uuid) : ast.lineOfKey("elements");
            throw new ModelParseException("Element missing 'from' coordinates", line, dto.uuid != null ? "/elements/" + dto.uuid : "/elements");
        }
        if (dto.to == null || dto.to.length != 3) {
            int line = dto.uuid != null ? ast.lineOfUuid(dto.uuid) : ast.lineOfKey("elements");
            throw new ModelParseException("Element missing 'to' coordinates", line, dto.uuid != null ? "/elements/" + dto.uuid : "/elements");
        }
        
        // Validation: from should be less than or equal to to
        // Note: After coordinate conversion, this might flip, but we handle it by recalculating min/max
        if (dto.from[0] > dto.to[0] || dto.from[1] > dto.to[1] || dto.from[2] > dto.to[2]) {
             int line = dto.uuid != null ? ast.lineOfUuid(dto.uuid) : ast.lineOfKey("elements");
             throw new ModelParseException("Element 'from' coordinates must be less than or equal to 'to' coordinates. UUID: " + dto.uuid, line, dto.uuid != null ? "/elements/" + dto.uuid : "/elements");
        }
    }

    private static ModelNode parseNode(JsonObject json, Map<UUID, ModelCube> elementMap, Map<String, JsonObject> groupMap, Set<UUID> usedCubes, ModelAst ast) {
        JsonObject source = json;
        String resolvedUuid = null;
        boolean isRefOnly = false;
        if (json.has("uuid") && !hasTransformFields(json)) {
            resolvedUuid = json.get("uuid").getAsString();
            JsonObject groupObj = groupMap.get(resolvedUuid);
            if (groupObj == null) {
                int line = ast.lineOfUuid(resolvedUuid);
                throw new ModelParseException("Reference to nonexistent group UUID: " + resolvedUuid, line, "/groups/" + resolvedUuid);
            }
            source = groupObj;
            isRefOnly = true;
        }

        String name = source.has("name") ? source.get("name").getAsString() : (source.has("uuid") ? source.get("uuid").getAsString() : "<group>");

        Vector3f origin = new Vector3f(0, 0, 0);
        if (source.has("origin")) {
            JsonArray originArr = source.getAsJsonArray("origin");
            if (originArr != null && originArr.size() == 3) {
                origin.set(
                    originArr.get(0).getAsFloat(),
                    originArr.get(1).getAsFloat(),
                    originArr.get(2).getAsFloat()
                );
            }
        }

        Vector3f position = new Vector3f(0, 0, 0);
        if (source.has("position")) {
            JsonArray posArr = source.getAsJsonArray("position");
            if (posArr != null && posArr.size() == 3) {
                position.set(
                    posArr.get(0).getAsFloat(),
                    posArr.get(1).getAsFloat(),
                    posArr.get(2).getAsFloat()
                );
            }
        }

        Vector3f rotation = new Vector3f(0, 0, 0);
        if (source.has("rotation")) {
            JsonArray rotArr = source.getAsJsonArray("rotation");
            if (rotArr != null && rotArr.size() == 3) {
                rotation.set(
                    rotArr.get(0).getAsFloat(),
                    rotArr.get(1).getAsFloat(),
                    rotArr.get(2).getAsFloat()
                );
            }
        }

        Vector3f scale = new Vector3f(1, 1, 1);
        if (source.has("scale")) {
            JsonElement scaleEl = source.get("scale");
            if (scaleEl.isJsonArray()) {
                JsonArray scaleArr = scaleEl.getAsJsonArray();
                if (scaleArr != null && scaleArr.size() == 3) {
                    scale.set(
                        scaleArr.get(0).getAsFloat(),
                        scaleArr.get(1).getAsFloat(),
                        scaleArr.get(2).getAsFloat()
                    );
                }
            }
        }

        ModelNode node = new ModelNode(name, origin, position, rotation, scale);

        JsonArray children = null;
        if (json.has("children")) {
            children = json.getAsJsonArray("children");
        } else if (source.has("children")) {
            children = source.getAsJsonArray("children");
        }

        if (children != null) {
            for (JsonElement child : children) {
                if (child.isJsonObject()) {
                    JsonObject childObj = child.getAsJsonObject();
                    if (childObj.has("uuid") && !hasTransformFields(childObj)) {
                        ModelNode childNode = parseNode(childObj, elementMap, groupMap, usedCubes, ast);
                        node.addChild(childNode);
                    } else {
                        ModelNode childNode = parseNode(childObj, elementMap, groupMap, usedCubes, ast);
                        node.addChild(childNode);
                    }
                } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    String uuidStr = child.getAsString();
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        int line = ast.lineOfUuid(uuidStr);
                        throw new ModelParseException("Invalid UUID format: " + uuidStr, line, "/outliner/" + uuidStr);
                    }

                    if (!elementMap.containsKey(uuid)) {
                        int line = ast.lineOfUuid(uuidStr);
                        throw new ModelParseException("Reference to nonexistent cube UUID: " + uuid, line, "/outliner/" + uuidStr);
                    }

                    if (usedCubes.contains(uuid)) {
                        int line = ast.lineOfUuid(uuidStr);
                        throw new ModelParseException("Cube referenced more than once: " + uuid, line, "/outliner/" + uuidStr);
                    }

                    ModelCube cube = elementMap.get(uuid);
                    node.addCube(cube);
                    usedCubes.add(uuid);
                } else {
                    throw new ModelParseException("Unknown child type in outliner: " + child, ast.lineOfKey("outliner"), "/outliner");
                }
            }
        }

        return node;
    }

    private static boolean hasTransformFields(JsonObject json) {
        return json.has("origin") || json.has("position") || json.has("rotation") || json.has("scale");
    }
}
