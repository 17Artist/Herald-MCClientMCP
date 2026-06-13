package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;

/** Port of BlackBoxPro advanced/RenameItemAction.kt. */
public final class RenameItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String name = JsonUtil.requireString(params, "name");
        if (name.length() > 50) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Name too long: " + name.length());
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundRenameItemPacket(name));
        return ActionResult.ok();
    }
}
