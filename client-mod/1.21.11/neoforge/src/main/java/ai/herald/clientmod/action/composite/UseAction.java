package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Optionally swap to a hotbar slot, then right-click in the air with a hand. */
public final class UseAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gm = mc.gameMode;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || gm == null || mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        if (conn == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));

        if (slot >= 0) {
            if (slot > 8) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "slot must be 0..8 (hotbar)");
            }
            if (McVersionCompat.getSelectedSlot(player.getInventory()) != slot) {
                McVersionCompat.setSelectedSlot(player.getInventory(), slot);
                conn.send(new ServerboundSetCarriedItemPacket(slot));
            }
        }

        InteractionResult result = gm.useItem(player, hand);

        JsonObject data = new JsonObject();
        data.addProperty("result", result.toString());
        data.addProperty("consumed", result.consumesAction());
        if (slot >= 0) data.addProperty("slot", slot);
        return ActionResult.ok(data);
    }
}
