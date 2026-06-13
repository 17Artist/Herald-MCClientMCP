package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

/** Port of BlackBoxPro QueryActiveEffectsAction.kt to Java + Mojang 1.20.1. */
public final class QueryActiveEffectsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        JsonArray arr = new JsonArray();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            ResourceLocation id = McVersionCompat.mobEffectIdFromInstance(instance);
            JsonObject e = new JsonObject();
            e.addProperty("id", id != null ? id.toString() : "unknown");
            e.addProperty("amplifier", instance.getAmplifier());
            e.addProperty("duration", instance.getDuration());
            e.addProperty("ambient", instance.isAmbient());
            e.addProperty("visible", instance.isVisible());
            arr.add(e);
        }

        JsonObject data = new JsonObject();
        data.add("effects", arr);
        return ActionResult.ok(data);
    }
}
