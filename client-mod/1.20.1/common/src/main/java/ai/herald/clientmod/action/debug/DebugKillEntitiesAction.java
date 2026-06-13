package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class DebugKillEntitiesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String type = JsonUtil.getStringOrDefault(params, "type", "");
        int radius = JsonUtil.getIntOrDefault(params, "radius", 32);

        Minecraft mc = McHelper.mc();

        if (mc.getSingleplayerServer() != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (sp == null) return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Cannot get server player");
            ServerLevel serverLevel = (ServerLevel) McVersionCompat.getServerLevel(sp);

            AABB box = new AABB(
                sp.getX() - radius, sp.getY() - radius, sp.getZ() - radius,
                sp.getX() + radius, sp.getY() + radius, sp.getZ() + radius
            );

            List<Entity> entities = serverLevel.getEntities(sp, box);
            List<Entity> toKill = new ArrayList<>();

            for (Entity e : entities) {
                if (e instanceof ServerPlayer) continue; // never kill players
                if (!type.isEmpty() && !type.equals("!player")) {
                    String fullType = type.contains(":") ? type : "minecraft:" + type;
                    EntityType<?> targetType = McVersionCompat.registryGet(BuiltInRegistries.ENTITY_TYPE, ResourceLocation.tryParse(fullType));
                    if (e.getType() != targetType) continue;
                }
                toKill.add(e);
            }

            for (Entity e : toKill) {
                e.discard();
            }

            JsonObject data = new JsonObject();
            data.addProperty("killed", toKill.size());
            data.addProperty("radius", radius);
            return ActionResult.ok(data);
        } else {
            String typeSelector = type.isEmpty() ? "!player" : type;
            String cmd = "kill @e[type=" + typeSelector + ",distance=.." + radius + "]";
            player.connection.sendCommand(cmd);
            JsonObject data = new JsonObject();
            data.addProperty("command", cmd);
            return ActionResult.ok(data);
        }
    }
}
