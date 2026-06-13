package ai.herald.clientmod.util;

import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HeraldException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * JSON parameter helpers. All {@code requireXxx} methods throw
 * {@link HeraldException} with {@link ErrorCode#INVALID_PARAMS} on failure
 * — the dispatcher unwraps this into the wire error response.
 */
public final class JsonUtil {

    private JsonUtil() {}

    public static String requireString(JsonObject obj, String key) {
        JsonElement el = require(obj, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw invalid(key, "string");
        }
        return el.getAsString();
    }

    public static String getStringOrDefault(JsonObject obj, String key, String dflt) {
        if (obj == null) return dflt;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return dflt;
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) return dflt;
        return el.getAsString();
    }

    public static int requireInt(JsonObject obj, String key) {
        JsonElement el = require(obj, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw invalid(key, "int");
        }
        return el.getAsInt();
    }

    public static int getIntOrDefault(JsonObject obj, String key, int dflt) {
        if (obj == null) return dflt;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return dflt;
        }
        return el.getAsInt();
    }

    public static long getLongOrDefault(JsonObject obj, String key, long dflt) {
        if (obj == null) return dflt;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return dflt;
        }
        return el.getAsLong();
    }

    public static double requireDouble(JsonObject obj, String key) {
        JsonElement el = require(obj, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw invalid(key, "double");
        }
        return el.getAsDouble();
    }

    public static double getDoubleOrDefault(JsonObject obj, String key, double dflt) {
        if (obj == null) return dflt;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return dflt;
        }
        return el.getAsDouble();
    }

    public static boolean requireBoolean(JsonObject obj, String key) {
        JsonElement el = require(obj, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw invalid(key, "boolean");
        }
        return el.getAsBoolean();
    }

    public static boolean getBooleanOrDefault(JsonObject obj, String key, boolean dflt) {
        if (obj == null) return dflt;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            return dflt;
        }
        return el.getAsBoolean();
    }

    public static com.google.gson.JsonArray getArrayOrEmpty(JsonObject obj, String key) {
        if (obj == null) return new com.google.gson.JsonArray();
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) return new com.google.gson.JsonArray();
        return el.getAsJsonArray();
    }

    public static JsonObject getObjectOrNull(JsonObject obj, String key) {
        if (obj == null) return null;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static JsonElement require(JsonObject obj, String key) {
        if (obj == null) {
            throw new HeraldException(ErrorCode.INVALID_PARAMS, "params object is null (missing '" + key + "')");
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            throw new HeraldException(ErrorCode.INVALID_PARAMS, "Missing required parameter: " + key);
        }
        return el;
    }

    private static HeraldException invalid(String key, String type) {
        return new HeraldException(ErrorCode.INVALID_PARAMS, "Parameter '" + key + "' must be a " + type);
    }
}
