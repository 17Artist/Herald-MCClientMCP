package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Trigger a single jump. {@code LivingEntity.jumpFromGround()} is protected
 * in 1.20.1, so we replicate its one-tick delta-Y impulse directly
 * (vanilla uses 0.42 m / tick for a baseline jump).
 */
public final class JumpAction implements ActionExecutor {

    private static final double JUMP_POWER = 0.42;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        if (!player.onGround()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Player not on ground");
        }
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, JUMP_POWER, v.z);
        player.hasImpulse = true;
        return ActionResult.ok();
    }
}
