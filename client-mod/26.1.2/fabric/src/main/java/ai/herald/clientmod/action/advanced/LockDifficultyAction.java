package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;

/** Port of BlackBoxPro advanced/LockDifficultyAction.kt. */
public final class LockDifficultyAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        boolean locked = JsonUtil.requireBoolean(params, "locked");
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundLockDifficultyPacket(locked));
        return ActionResult.ok();
    }
}
