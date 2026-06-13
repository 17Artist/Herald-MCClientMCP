package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;

public class ConfigListAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String modId = JsonUtil.getStringOrDefault(params, "modId", null);

        Minecraft mc = McHelper.mc();
        File configDir = new File(mc.gameDirectory, "config");

        if (!configDir.exists() || !configDir.isDirectory()) {
            JsonObject data = new JsonObject();
            data.addProperty("count", 0);
            data.add("files", new JsonArray());
            data.addProperty("note", "Config directory does not exist");
            return ActionResult.ok(data);
        }

        File[] files = configDir.listFiles();
        if (files == null) files = new File[0];

        JsonArray fileList = new JsonArray();
        for (File file : files) {
            if (file.isDirectory()) continue;
            if (modId != null && !file.getName().toLowerCase().contains(modId.toLowerCase())) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("filename", file.getName());
            entry.addProperty("size", file.length());
            entry.addProperty("lastModified", file.lastModified());
            fileList.add(entry);
        }

        JsonObject data = new JsonObject();
        data.addProperty("count", fileList.size());
        data.addProperty("configDir", configDir.getAbsolutePath());
        data.add("files", fileList);
        return ActionResult.ok(data);
    }
}
