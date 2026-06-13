package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;

public class WorldListAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        File savesDir = new File(mc.gameDirectory, "saves");

        if (!savesDir.exists() || !savesDir.isDirectory()) {
            JsonObject data = new JsonObject();
            data.addProperty("count", 0);
            data.add("worlds", new JsonArray());
            return ActionResult.ok(data);
        }

        File[] worldDirs = savesDir.listFiles(File::isDirectory);
        if (worldDirs == null) worldDirs = new File[0];

        JsonArray worlds = new JsonArray();
        for (File worldDir : worldDirs) {
            File levelDat = new File(worldDir, "level.dat");
            if (!levelDat.exists()) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("name", worldDir.getName());
            entry.addProperty("lastPlayed", levelDat.lastModified());
            entry.addProperty("size", calculateDirSize(worldDir));
            worlds.add(entry);
        }

        JsonObject data = new JsonObject();
        data.addProperty("count", worlds.size());
        data.addProperty("savesDir", savesDir.getAbsolutePath());
        data.add("worlds", worlds);
        return ActionResult.ok(data);
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
