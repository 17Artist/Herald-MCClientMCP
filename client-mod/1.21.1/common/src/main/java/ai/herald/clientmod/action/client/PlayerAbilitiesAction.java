package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;

/** Port of BlackBoxPro client/PlayerAbilitiesAction.kt. */
public final class PlayerAbilitiesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        boolean flying = JsonUtil.requireBoolean(params, "flying");
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();
        if (flying && !player.getAbilities().mayfly) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Player is not allowed to fly");
        }
        player.getAbilities().flying = flying;
        conn.send(new ServerboundPlayerAbilitiesPacket(player.getAbilities()));
        return ActionResult.ok();
    }
}
