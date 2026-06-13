package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Sync: finds the nearest hostile mob (or entity of given type) and attacks it.
 */
public final class CombatTargetNearestAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null) return McHelper.notInGame();

        String type = JsonUtil.getStringOrDefault(params, "type", "monster");
        double maxRange = JsonUtil.getDoubleOrDefault(params, "maxRange", 16.0);
        if (maxRange <= 0) maxRange = 16.0;

        // Normalize type to full Identifier form (e.g. "pig" → "minecraft:pig")
        String normalizedType = type;
        if (!"monster".equals(type) && !type.contains(":")) {
            normalizedType = "minecraft:" + type;
        }

        AABB box = player.getBoundingBox().inflate(maxRange);

        // Retry loop: in singleplayer, entities spawned by commands may take 1-2 ticks to sync to client
        List<Entity> filtered = new ArrayList<>();
        for (int attempt = 0; attempt < 5 && filtered.isEmpty(); attempt++) {
            if (attempt > 0) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
            List<Entity> candidates = level.getEntities(player, box);
            for (Entity e : candidates) {
                if ("monster".equals(type)) {
                    if (e instanceof Monster) filtered.add(e);
                } else {
                    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                    if (id != null && id.toString().equals(normalizedType)) filtered.add(e);
                }
            }
        }

        if (filtered.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity of type '" + type + "' within range " + maxRange);
        }

        filtered.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        Entity target = filtered.get(0);

        // Look at target
        double dx = target.getX() - player.getX();
        double dy = (target.getEyeY()) - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        player.setYRot(yaw);
        player.setXRot(pitch);

        // Attack
        gm.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);

        JsonObject data = new JsonObject();
        data.addProperty("entity_id", target.getId());
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        data.addProperty("entity_type", typeId != null ? typeId.toString() : "unknown");
        data.addProperty("distance", Math.sqrt(target.distanceToSqr(player)));
        if (target instanceof LivingEntity le) {
            data.addProperty("health", le.getHealth());
        }
        return ActionResult.ok(data);
    }
}
