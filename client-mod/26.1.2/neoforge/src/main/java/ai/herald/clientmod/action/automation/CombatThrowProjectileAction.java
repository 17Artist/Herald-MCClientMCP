package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Sync: sets look angle, optionally switches hotbar slot, then uses item (throw projectile).
 */
public final class CombatThrowProjectileAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        float yaw = (float) JsonUtil.requireDouble(params, "yaw");
        float pitch = (float) JsonUtil.requireDouble(params, "pitch");
        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);

        // Set look direction
        player.setYRot(yaw);
        player.setXRot(pitch);

        // Switch hotbar slot if specified
        if (slot >= 0 && slot <= 8) {
            McVersionCompat.setSelectedSlot(player.getInventory(), slot);
        }

        // Use item
        Minecraft mc = McHelper.mc();
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);

        JsonObject data = new JsonObject();
        data.addProperty("yaw", yaw);
        data.addProperty("pitch", pitch);
        data.addProperty("slot", McVersionCompat.getSelectedSlot(player.getInventory()));
        return ActionResult.ok(data);
    }
}
