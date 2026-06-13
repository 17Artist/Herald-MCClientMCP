package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

public final class DebugSummonEntityAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String entityType = JsonUtil.requireString(params, "entityType");
        if (!entityType.contains(":")) entityType = "minecraft:" + entityType;
        double x = JsonUtil.getDoubleOrDefault(params, "x", player.getX());
        double y = JsonUtil.getDoubleOrDefault(params, "y", player.getY());
        double z = JsonUtil.getDoubleOrDefault(params, "z", player.getZ());

        Minecraft mc = McHelper.mc();
        JsonObject data = new JsonObject();

        // In singleplayer: spawn directly on server (bypasses cheats)
        if (mc.getSingleplayerServer() != null) {
            ResourceLocation rl = ResourceLocation.parse(entityType);
            Optional<EntityType<?>> type = EntityType.byString(rl.toString());
            if (type.isPresent()) {
                String dim = player.level().dimension().location().toString();
                ServerLevel serverLevel = null;
                for (ServerLevel sl : mc.getSingleplayerServer().getAllLevels()) {
                    if (sl.dimension().location().toString().equals(dim)) {
                        serverLevel = sl;
                        break;
                    }
                }
                if (serverLevel != null) {
                    final ServerLevel level = serverLevel;
                    final double fx = x, fy = y, fz = z;
                    final EntityType<?> et = type.get();
                    mc.getSingleplayerServer().execute(() -> {
                        Entity entity = et.create(level, EntitySpawnReason.COMMAND);
                        if (entity != null) {
                            entity.snapTo(fx, fy, fz, 0f, 0f);
                            level.addFreshEntity(entity);
                        }
                    });
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    data.addProperty("spawned", true);
                    data.addProperty("entityType", entityType);
                }
            }
        } else {
            // Multiplayer: use command
            String cmd = "summon " + entityType + " " + x + " " + y + " " + z;
            player.connection.sendCommand(cmd);
            data.addProperty("command", cmd);
        }

        return ActionResult.ok(data);
    }
}
