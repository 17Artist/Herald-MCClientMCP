package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Return recent damage events.
 * Note: The client does not maintain a damage history buffer by default.
 * This would require a Mixin or event hook to track damage.
 * Returns empty array with a note about needing event hooks.
 */
public final class QueryDamageHistoryAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        @SuppressWarnings("unused")
        int count = JsonUtil.getIntOrDefault(params, "count", 10);

        // Current health info as a fallback
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();

        JsonObject data = new JsonObject();
        data.add("events", new JsonArray());
        data.addProperty("currentHealth", health);
        data.addProperty("maxHealth", maxHealth);
        data.addProperty("absorption", absorption);
        data.addProperty("note",
                "Damage history tracking requires Mixin hooks into LivingEntity.hurt. " +
                "Currently returns empty; install sensing hooks for full tracking.");
        return ActionResult.ok(data);
    }
}
