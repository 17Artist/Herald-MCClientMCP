package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Sync check: look for an entity of the given type within radius.
 * Returns ok + entity info if found, error ASSERTION_FAILED if not.
 */
public final class WaitEntitySpawnAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String type = JsonUtil.requireString(params, "type");
        double radius = JsonUtil.getDoubleOrDefault(params, "radius", 16.0);

        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> entities = level.getEntities(player, box);

        for (Entity entity : entities) {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String entityType = entityId != null ? entityId.toString() : "unknown";

            if (entityType.equals(type) || entityType.equals("minecraft:" + type)) {
                JsonObject data = new JsonObject();
                data.addProperty("found", true);
                data.addProperty("type", entityType);
                data.addProperty("entityId", entity.getId());
                data.addProperty("uuid", entity.getStringUUID());
                data.addProperty("x", entity.getX());
                data.addProperty("y", entity.getY());
                data.addProperty("z", entity.getZ());
                data.addProperty("distance", Math.sqrt(entity.distanceToSqr(player)));
                if (entity instanceof LivingEntity le) {
                    data.addProperty("health", le.getHealth());
                }
                return ActionResult.ok(data);
            }
        }

        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Entity not found: type=" + type + " within radius=" + radius);
    }
}
