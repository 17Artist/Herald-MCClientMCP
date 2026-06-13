package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Port of BlackBoxPro QueryNearbyEntitiesAction.kt to Java + Mojang 1.20.1. */
public final class QueryNearbyEntitiesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        double radius = clamp(JsonUtil.getDoubleOrDefault(params, "radius", 10.0), 0.1, 64.0);
        String typeFilter = JsonUtil.getStringOrDefault(params, "type", null);
        int limit = (int) clamp(JsonUtil.getIntOrDefault(params, "limit", 20), 1, 100);

        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> raw = level.getEntities(player, box);

        List<Entity> entities = new ArrayList<>();
        for (Entity e : raw) {
            if (typeFilter != null) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                if (id == null || !id.toString().equals(typeFilter)) continue;
            }
            entities.add(e);
        }
        entities.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        if (entities.size() > limit) entities = entities.subList(0, limit);

        JsonArray arr = new JsonArray();
        for (Entity entity : entities) {
            double distSq = entity.distanceToSqr(player);
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            JsonObject o = new JsonObject();
            o.addProperty("entityId", entity.getId());
            o.addProperty("uuid", entity.getStringUUID());
            o.addProperty("type", typeId != null ? typeId.toString() : "unknown");
            o.addProperty("name", entity.getName().getString());
            o.addProperty("x", entity.getX());
            o.addProperty("y", entity.getY());
            o.addProperty("z", entity.getZ());
            o.addProperty("distance", Math.sqrt(distSq));
            if (entity instanceof LivingEntity le) {
                o.addProperty("health", le.getHealth());
                o.addProperty("maxHealth", le.getMaxHealth());
            }
            arr.add(o);
        }

        JsonObject data = new JsonObject();
        data.add("entities", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
