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

public class WorldRestoreAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String backupName = JsonUtil.getStringOrDefault(params, "backupName", null);

        if (backupName == null || backupName.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "backupName is required");
        }

        if (backupName.contains("..") || backupName.contains("/") || backupName.contains("\\")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid backup name: path traversal not allowed");
        }

        Minecraft mc = McHelper.mc();
        File savesDir = new File(mc.gameDirectory, "saves");
        File backupDir = new File(savesDir, backupName);

        if (!backupDir.exists()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Backup not found: " + backupName);
        }

        String originalName = backupName.replaceAll("_backup_\\d{8}_\\d{6}$", "");
        File targetDir = new File(savesDir, originalName);

        if (targetDir.exists()) {
            deleteRecursive(targetDir);
        }

        try {
            copyDirectory(backupDir.toPath(), targetDir.toPath());

            JsonObject data = new JsonObject();
            data.addProperty("restored", true);
            data.addProperty("backupName", backupName);
            data.addProperty("restoredTo", originalName);
            data.addProperty("path", targetDir.getAbsolutePath());
            return ActionResult.ok(data);
        } catch (IOException e) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Restore failed: " + e.getMessage());
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

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
