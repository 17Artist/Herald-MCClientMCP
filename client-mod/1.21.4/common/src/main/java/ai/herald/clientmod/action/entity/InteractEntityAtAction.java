package ai.herald.clientmod.action.entity;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Port of BlackBoxPro entity/InteractEntityAtAction.kt. */
public final class InteractEntityAtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");
        double x = JsonUtil.requireDouble(params, "targetX");
        double y = JsonUtil.requireDouble(params, "targetY");
        double z = JsonUtil.requireDouble(params, "targetZ");
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));
        boolean sneaking = JsonUtil.getBooleanOrDefault(params, "sneaking", false);

        ClientLevel level = McHelper.level();
        ClientPacketListener conn = McHelper.connection();
        if (level == null || conn == null) return McHelper.notInGame();
        Entity target = level.getEntity(entityId);
        if (target == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);

        conn.send(ServerboundInteractPacket.createInteractionPacket(target, sneaking, hand, new Vec3(x, y, z)));
        return ActionResult.ok();
    }
}
