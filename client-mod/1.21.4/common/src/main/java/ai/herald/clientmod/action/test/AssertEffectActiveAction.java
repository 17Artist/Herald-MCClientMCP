package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

/**
 * Asserts that the player has an active mob effect with at least the specified level and duration.
 */
public final class AssertEffectActiveAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String effectId = JsonUtil.requireString(params, "effectId");
        // Normalize: "speed" → "minecraft:speed"
        if (!effectId.contains(":")) {
            effectId = "minecraft:" + effectId;
        }
        int minLevel = JsonUtil.getIntOrDefault(params, "minLevel", 0);
        int minDuration = JsonUtil.getIntOrDefault(params, "minDuration", 0);

        // Collect active effects; prefer integrated server data in singleplayer (avoids sync delay)
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        Minecraft mc = McHelper.mc();
        if (mc.getSingleplayerServer() != null && mc.player != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            if (sp != null) {
                effects = sp.getActiveEffects();
            }
        }

        for (MobEffectInstance instance : effects) {
            ResourceLocation id = McVersionCompat.mobEffectIdFromInstance(instance);
            if (id != null && id.toString().equals(effectId)) {
                int amplifier = instance.getAmplifier();
                int duration = instance.getDuration();
                if (amplifier < minLevel) {
                    return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                        "Effect " + effectId + " found but level " + amplifier + " < required " + minLevel);
                }
                if (duration < minDuration) {
                    return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                        "Effect " + effectId + " found but duration " + duration + " < required " + minDuration);
                }
                JsonObject data = new JsonObject();
                data.addProperty("pass", true);
                data.addProperty("message", "Effect " + effectId + " active (level=" + amplifier + ", duration=" + duration + ")");
                return ActionResult.ok(data);
            }
        }

        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
            "Expected active effect " + effectId + " but player does not have it");
    }
}
