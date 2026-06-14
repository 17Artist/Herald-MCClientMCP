package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sync: Toggle a lever at the given position by interacting with it.
 * Params: x, y, z
 */
public final class RedstoneToggleLeverAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        MultiPlayerGameMode gm = McHelper.gameMode();
        if (player == null || level == null || gm == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof LeverBlock)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "No lever at (" + x + "," + y + "," + z + ")");
        }

        Vec3 cursor = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(cursor, Direction.UP, pos, false);
        InteractionResult result = gm.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        JsonObject data = new JsonObject();
        data.addProperty("toggled", result.consumesAction());
        return ActionResult.ok(data);
    }
}
