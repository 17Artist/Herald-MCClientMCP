package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Begin sneaking — sets the player flag locally and notifies the server. */
public final class SneakStartAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        player.setShiftKeyDown(true);
        return ActionResult.ok();
    }
}
