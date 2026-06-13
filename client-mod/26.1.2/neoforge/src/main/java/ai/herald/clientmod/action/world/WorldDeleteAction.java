package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;

public class WorldDeleteAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String name = JsonUtil.getStringOrDefault(params, "name", null);

        if (name == null || name.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "name is required");
        }

        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid world name: path traversal not allowed");
        }

        Minecraft mc = McHelper.mc();
        File savesDir = new File(mc.gameDirectory, "saves");
        File worldDir = new File(savesDir, name);

        if (!worldDir.exists()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "World not found: " + name);
        }

        if (!worldDir.isDirectory()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Not a directory: " + name);
        }

        long size = calculateDirSize(worldDir);
        boolean deleted = deleteRecursive(worldDir);

        JsonObject data = new JsonObject();
        data.addProperty("deleted", deleted);
        data.addProperty("name", name);
        data.addProperty("path", worldDir.getAbsolutePath());
        data.addProperty("freedBytes", size);
        return ActionResult.ok(data);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private long calculateDirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
}
