package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Set the player's yaw/pitch. The next physics tick relays it to the server. */
public final class PlayerLookAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        float yaw = (float) JsonUtil.requireDouble(params, "yaw");
        float pitch = (float) JsonUtil.requireDouble(params, "pitch");
        player.setYRot(yaw);
        player.setXRot(pitch);
        // Snap previous tick state so interpolation matches.
        player.yRotO = yaw;
        player.xRotO = pitch;
        JsonObject data = new JsonObject();
        data.addProperty("yaw", yaw);
        data.addProperty("pitch", pitch);
        return ActionResult.ok(data);
    }
}
