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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class QueryEntitiesInAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        double x1 = JsonUtil.requireDouble(params, "x1");
        double y1 = JsonUtil.requireDouble(params, "y1");
        double z1 = JsonUtil.requireDouble(params, "z1");
        double x2 = JsonUtil.requireDouble(params, "x2");
        double y2 = JsonUtil.requireDouble(params, "y2");
        double z2 = JsonUtil.requireDouble(params, "z2");

        AABB area = new AABB(x1, y1, z1, x2, y2, z2);
        List<Entity> entities = level.getEntities(null, area);

        JsonArray arr = new JsonArray();
        for (Entity entity : entities) {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            JsonObject o = new JsonObject();
            o.addProperty("id", entity.getId());
            o.addProperty("type", typeId != null ? typeId.toString() : "unknown");
            o.addProperty("x", entity.getX());
            o.addProperty("y", entity.getY());
            o.addProperty("z", entity.getZ());
            if (entity instanceof LivingEntity le) {
                o.addProperty("health", le.getHealth());
                o.addProperty("maxHealth", le.getMaxHealth());
            }
            if (entity.hasCustomName()) {
                o.addProperty("customName", entity.getCustomName().getString());
            }
            arr.add(o);
        }

        JsonObject data = new JsonObject();
        data.add("entities", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
