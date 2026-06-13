package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Sync: Count all entities in the loaded level.
 * If byType=true, group by entity type and return counts per type.
 */
public final class QueryEntityCountAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        boolean byType = JsonUtil.getBooleanOrDefault(params, "byType", false);

        int total = 0;
        Map<String, Integer> typeCounts = byType ? new HashMap<>() : null;

        for (Entity entity : level.entitiesForRendering()) {
            total++;
            if (byType) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String key = id != null ? id.toString() : "unknown";
                typeCounts.merge(key, 1, Integer::sum);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("total", total);

        if (byType && typeCounts != null) {
            JsonObject types = new JsonObject();
            typeCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> types.addProperty(e.getKey(), e.getValue()));
            data.add("byType", types);
        }

        return ActionResult.ok(data);
    }
}
