package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.MathUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;

/** Aim at the eye-position of a target entity (resolved by id). */
public final class LookAtEntityAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || level == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int entityId = JsonUtil.requireInt(params, "entityId");
        Entity target = level.getEntity(entityId);
        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }

        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
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
        data.addProperty("entity_id", entityId);
        return ActionResult.ok(data);
    }
}
