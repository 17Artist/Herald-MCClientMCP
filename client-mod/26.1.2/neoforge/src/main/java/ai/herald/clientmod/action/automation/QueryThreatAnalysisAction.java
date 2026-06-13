package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Sync: lists all hostile mobs within radius with distance, health, type.
 * Sorted by distance ascending.
 */
public final class QueryThreatAnalysisAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        double radius = JsonUtil.getDoubleOrDefault(params, "radius", 16.0);
        if (radius <= 0) radius = 16.0;
        if (radius > 64.0) radius = 64.0;

        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> entities = level.getEntities(player, box);

        List<Entity> threats = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof Monster && e.isAlive()) {
                threats.add(e);
            }
        }
        threats.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));

        JsonArray arr = new JsonArray();
        for (Entity e : threats) {
            JsonObject t = new JsonObject();
            t.addProperty("entity_id", e.getId());
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            t.addProperty("type", typeId != null ? typeId.toString() : "unknown");
            t.addProperty("name", e.getName().getString());
            t.addProperty("x", e.getX());
            t.addProperty("y", e.getY());
            t.addProperty("z", e.getZ());
            t.addProperty("distance", Math.sqrt(e.distanceToSqr(player)));
            if (e instanceof LivingEntity le) {
                t.addProperty("health", le.getHealth());
                t.addProperty("max_health", le.getMaxHealth());
            }
            arr.add(t);
        }

        JsonObject data = new JsonObject();
        data.add("threats", arr);
        data.addProperty("count", arr.size());
        data.addProperty("radius", radius);
        return ActionResult.ok(data);
    }
}
