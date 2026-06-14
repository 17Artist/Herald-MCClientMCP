package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.DirectionUtil;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Aim at the centre of the chosen face of a block. */
public final class LookAtBlockAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null)   return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        Direction face = DirectionUtil.fromString(JsonUtil.getStringOrDefault(params, "face", "up"));

        double cx = x + 0.5 + face.getStepX() * 0.5;
        double cy = y + 0.5 + face.getStepY() * 0.5;
        double cz = z + 0.5 + face.getStepZ() * 0.5;

        double dx = cx - player.getX();
        double dy = cy - player.getEyeY();
        double dz = cz - player.getZ();
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
        data.addProperty("face", face.getName());
        data.addProperty("targetX", cx);
        data.addProperty("targetY", cy);
        data.addProperty("targetZ", cz);
        return ActionResult.ok(data);
    }
}
