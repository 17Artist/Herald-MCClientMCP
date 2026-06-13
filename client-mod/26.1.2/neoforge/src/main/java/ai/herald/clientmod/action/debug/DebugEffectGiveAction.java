package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;


public final class DebugEffectGiveAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String effectId = JsonUtil.requireString(params, "effectId");
        if (!effectId.contains(":")) effectId = "minecraft:" + effectId;
        int duration = JsonUtil.getIntOrDefault(params, "duration", 60);
        int amplifier = JsonUtil.getIntOrDefault(params, "amplifier", 0);

        Minecraft mc = McHelper.mc();

        // In singleplayer: directly apply effect on server player (bypasses cheats)
        if (mc.getSingleplayerServer() != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (sp != null) {
                Identifier rl = Identifier.tryParse(effectId);
                if (rl != null && McVersionCompat.addMobEffect(sp, rl, duration * 20, amplifier)) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }
        } else {
            String cmd = "effect give @s " + effectId + " " + duration + " " + amplifier;
            player.connection.sendCommand(cmd);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        JsonObject data = new JsonObject();
        data.addProperty("command", "effect give @s " + effectId + " " + duration + " " + amplifier);
        return ActionResult.ok(data);
    }
}
