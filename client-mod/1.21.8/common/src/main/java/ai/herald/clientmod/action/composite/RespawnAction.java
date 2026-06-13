package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/** Click "Respawn" — sends {@link ServerboundClientCommandPacket.Action#PERFORM_RESPAWN}. */
public final class RespawnAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null)   return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        conn.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        return ActionResult.ok();
    }
}
