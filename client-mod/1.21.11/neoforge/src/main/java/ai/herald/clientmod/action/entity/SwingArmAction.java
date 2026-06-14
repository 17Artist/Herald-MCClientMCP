package ai.herald.clientmod.action.entity;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Port of BlackBoxPro entity/SwingArmAction.kt — client swing + auto-sends ServerboundSwingPacket. */
public final class SwingArmAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();
        player.swing(hand);
        return ActionResult.ok();
    }
}
