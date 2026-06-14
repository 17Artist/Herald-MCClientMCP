package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Look at a block, then right-click it to open its GUI (chest, furnace, …).
 * The look step is performed inline; the interact step uses the existing
 * place_block packet path via {@link MultiPlayerGameMode#useItemOn}.
 */
public final class OpenContainerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));

        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No block at (" + x + "," + y + "," + z + ")");
        }

        // Aim at top-face centre — small composite step.
        JsonObject lookParams = new JsonObject();
        lookParams.addProperty("x", x);
        lookParams.addProperty("y", y);
        lookParams.addProperty("z", z);
        lookParams.addProperty("face", "up");
        new LookAtBlockAction().execute(lookParams);

        Vec3 cursor = new Vec3(x + 0.5, y + 1.0, z + 0.5);
        BlockHitResult hit = new BlockHitResult(cursor, Direction.UP, pos, false);
        InteractionResult result = gm.useItemOn(player, hand, hit);

        JsonObject data = new JsonObject();
        data.addProperty("result", result.toString());
        data.addProperty("consumed", result.consumesAction());
        return ActionResult.ok(data);
    }
}
