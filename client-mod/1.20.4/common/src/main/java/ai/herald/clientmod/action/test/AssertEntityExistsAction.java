package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Asserts that at least minCount entities of the given type exist within radius of the player.
 */
public final class AssertEntityExistsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        String type = JsonUtil.requireString(params, "type");
        double radius = JsonUtil.getDoubleOrDefault(params, "radius", 16.0);
        int minCount = JsonUtil.getIntOrDefault(params, "minCount", 1);

        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> entities = level.getEntities(null, box);

        int count = 0;
        for (Entity e : entities) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (id != null && id.toString().equals(type)) {
                count++;
            }
        }

        if (count < minCount) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected at least " + minCount + " entities of type " + type
                    + " within radius " + radius + " but found " + count);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Found " + count + " entities of type " + type + " within radius " + radius);
        return ActionResult.ok(data);
    }
}
