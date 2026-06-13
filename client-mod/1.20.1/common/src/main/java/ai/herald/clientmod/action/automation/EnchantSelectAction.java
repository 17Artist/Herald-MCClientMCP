package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.inventory.EnchantmentMenu;

/**
 * Sync: Select an enchantment option (slot 0-2) from an open enchanting table.
 * Params: slot (int, 0-2)
 */
public final class EnchantSelectAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int slot = JsonUtil.requireInt(params, "slot");
        if (slot < 0 || slot > 2) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "slot must be 0, 1, or 2");
        }

        if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No enchanting table GUI is open");
        }

        if (menu.costs[slot] <= 0) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No enchantment available in slot " + slot);
        }

        conn.send(new ServerboundContainerButtonClickPacket(menu.containerId, slot));

        JsonObject data = new JsonObject();
        data.addProperty("slot", slot);
        data.addProperty("selected", true);
        return ActionResult.ok(data);
    }
}
