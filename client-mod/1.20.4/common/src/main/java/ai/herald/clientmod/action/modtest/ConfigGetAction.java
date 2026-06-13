package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigGetAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String modId = JsonUtil.getStringOrDefault(params, "modId", null);
        String key = JsonUtil.getStringOrDefault(params, "key", null);

        if (modId == null || modId.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "modId is required");
        }
        if (key == null || key.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "key is required");
        }

        Minecraft mc = McHelper.mc();
        File configDir = new File(mc.gameDirectory, "config");

        File configFile = findConfigFile(configDir, modId);
        if (configFile == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No config file found for modId: " + modId);
        }

        try {
            String content = Files.readString(configFile.toPath());
            String value = extractValue(content, key, configFile.getName());

            JsonObject data = new JsonObject();
            data.addProperty("modId", modId);
            data.addProperty("key", key);
            data.addProperty("configFile", configFile.getName());
            if (value != null) {
                data.addProperty("value", value);
            } else {
                data.addProperty("value", (String) null);
                data.addProperty("note", "Key not found in config file");
            }
            return ActionResult.ok(data);
        } catch (IOException e) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Failed to read config: " + e.getMessage());
        }
    }

    private File findConfigFile(File configDir, String modId) {
        String[] extensions = {".json", ".toml", ".yml", ".yaml", ".properties", ".conf"};
        for (String ext : extensions) {
            File file = new File(configDir, modId + ext);
            if (file.exists()) return file;
        }
        File[] files = configDir.listFiles((dir, name) -> name.startsWith(modId));
        if (files != null && files.length > 0) return files[0];
        return null;
    }

    private String extractValue(String content, String key, String filename) {
        if (filename.endsWith(".json")) {
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                if (json.has(key)) return json.get(key).toString();
            } catch (Exception ignored) {}
        } else if (filename.endsWith(".properties")) {
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                    return line.substring(line.indexOf('=') + 1).trim();
                }
            }
        } else {
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.startsWith(key + "=") || line.startsWith(key + " =")
                        || line.startsWith(key + ":") || line.startsWith(key + " :")) {
                    int sep = line.indexOf('=');
                    if (sep < 0) sep = line.indexOf(':');
                    if (sep >= 0) return line.substring(sep + 1).trim();
                }
            }
        }
        return null;
    }
}
