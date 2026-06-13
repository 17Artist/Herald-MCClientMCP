package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Stop sneaking. */
public final class SneakStopAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        player.setShiftKeyDown(false);
        mc.getConnection().send(new ServerboundPlayerCommandPacket(player, Action.RELEASE_SHIFT_KEY));
        return ActionResult.ok();
    }
}
