package ai.herald.clientmod.action.task;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class GoalKillEntityAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        String type = JsonUtil.getStringOrDefault(params, "type", null);
        if (type == null || type.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: type");
        }

        int count = params.has("count") ? params.get("count").getAsInt() : 1;
        double radius = params.has("radius") ? params.get("radius").getAsDouble() : 32.0;

        ResourceLocation typeRL = ResourceLocation.tryParse(type);
        Optional<EntityType<?>> entityTypeOpt = EntityType.byString(typeRL.toString());

        if (entityTypeOpt.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown entity type: " + type);
        }

        EntityType<?> entityType = entityTypeOpt.get();
        Vec3 playerPos = player.position();
        AABB searchBox = new AABB(
                playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
        );

        List<Entity> entities = mc.level.getEntities(player, searchBox, e -> e.getType() == entityType);

        // Find nearest
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            double dist = e.distanceTo(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = e;
            }
        }

        // Create task
        JsonArray steps = new JsonArray();
        steps.add("kill_entity");
        String taskId = TaskManager.create("kill_" + type.replace(":", "_"), steps, "abort");

        JsonObject data = new JsonObject();
        data.addProperty("taskId", taskId);
        data.addProperty("entityType", type);
        data.addProperty("targetCount", count);
        data.addProperty("found", entities.size());
        data.addProperty("radius", radius);

        if (nearest != null) {
            JsonObject nearestObj = new JsonObject();
            nearestObj.addProperty("id", nearest.getId());
            nearestObj.addProperty("distance", Math.round(nearestDist * 100.0) / 100.0);
            if (nearest instanceof LivingEntity living) {
                nearestObj.addProperty("health", living.getHealth());
                nearestObj.addProperty("maxHealth", living.getMaxHealth());
            }
            nearestObj.addProperty("x", Math.round(nearest.getX() * 10.0) / 10.0);
            nearestObj.addProperty("y", Math.round(nearest.getY() * 10.0) / 10.0);
            nearestObj.addProperty("z", Math.round(nearest.getZ() * 10.0) / 10.0);
            data.add("nearest", nearestObj);
        }

        return ActionResult.ok(data);
    }
}
