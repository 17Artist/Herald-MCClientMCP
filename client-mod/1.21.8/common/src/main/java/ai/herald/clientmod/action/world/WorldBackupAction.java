package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WorldBackupAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String name = JsonUtil.getStringOrDefault(params, "name", null);

        Minecraft mc = McHelper.mc();
        File savesDir = new File(mc.gameDirectory, "saves");

        if (name == null || name.isEmpty()) {
            if (mc.level != null && mc.getSingleplayerServer() != null) {
                name = mc.getSingleplayerServer().getWorldData().getLevelName();
            } else {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "name is required when not in a singleplayer world");
            }
        }

        File worldDir = new File(savesDir, name);
        if (!worldDir.exists()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "World not found: " + name);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupName = name + "_backup_" + timestamp;
        File backupDir = new File(savesDir, backupName);

        try {
            copyDirectory(worldDir.toPath(), backupDir.toPath());
            long size = calculateDirSize(backupDir);

            JsonObject data = new JsonObject();
            data.addProperty("backed_up", true);
            data.addProperty("worldName", name);
            data.addProperty("backupName", backupName);
            data.addProperty("backupPath", backupDir.getAbsolutePath());
            data.addProperty("size", size);
            return ActionResult.ok(data);
        } catch (IOException e) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Backup failed: " + e.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
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
