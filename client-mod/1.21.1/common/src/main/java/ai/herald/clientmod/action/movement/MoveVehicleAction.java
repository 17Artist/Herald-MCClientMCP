package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;

/** Port of BlackBoxPro movement/MoveVehicleAction.kt. */
public final class MoveVehicleAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        double x = JsonUtil.requireDouble(params, "x");
        double y = JsonUtil.requireDouble(params, "y");
        double z = JsonUtil.requireDouble(params, "z");
        float yaw = (float) JsonUtil.requireDouble(params, "yaw");
        float pitch = (float) JsonUtil.requireDouble(params, "pitch");

        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();
        Entity vehicle = player.getVehicle();
        if (vehicle == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Player has no vehicle");

        vehicle.setPos(x, y, z);
        vehicle.setYRot(yaw);
        vehicle.setXRot(pitch);
        McVersionCompat.sendMoveVehiclePacket(conn, vehicle);
        return ActionResult.ok();
    }
}
