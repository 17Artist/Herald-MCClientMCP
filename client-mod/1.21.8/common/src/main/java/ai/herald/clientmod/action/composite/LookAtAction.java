package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Look at a world-space point. Sets yaw/pitch and notifies the server. */
public final class LookAtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null)   return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        double tx = JsonUtil.requireDouble(params, "x");
        double ty = JsonUtil.requireDouble(params, "y");
        double tz = JsonUtil.requireDouble(params, "z");

        double dx = tx - player.getX();
        double dy = ty - player.getEyeY();
        double dz = tz - player.getZ();
        float yaw   = MathUtil.yawTo(dx, dz);
        float pitch = MathUtil.pitchTo(dx, dy, dz);

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        McVersionCompat.sendRotPacket(conn, yaw, pitch, player.onGround());

        JsonObject data = new JsonObject();
        data.addProperty("yaw", yaw);
        data.addProperty("pitch", pitch);
        return ActionResult.ok(data);
    }
}
