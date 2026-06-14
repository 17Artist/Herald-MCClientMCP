package ai.herald.clientmod.action.block;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Right-click with an item in the air (e.g. draw bow, eat food, throw pearl). */
public final class UseItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || gm == null || mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));
        InteractionResult result = gm.useItem(player, hand);
        JsonObject data = new JsonObject();
        data.addProperty("result", result.toString());
        data.addProperty("consumed", result.consumesAction());
        return ActionResult.ok(data);
    }
}
