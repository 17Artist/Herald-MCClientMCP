package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

/** Port of BlackBoxPro advanced/UpdateSignAction.kt. */
public final class UpdateSignAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        boolean isFrontText = JsonUtil.getBooleanOrDefault(params, "isFrontText", true);
        JsonArray linesArr = JsonUtil.getArrayOrEmpty(params, "lines");
        if (linesArr.size() == 0) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required field: lines");
        }
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = i < linesArr.size() ? linesArr.get(i).getAsString() : "";
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundSignUpdatePacket(new BlockPos(x, y, z), isFrontText,
            lines[0], lines[1], lines[2], lines[3]));
        return ActionResult.ok();
    }
}
