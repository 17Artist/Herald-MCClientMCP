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
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sync: Interact with a brewing stand to open its GUI.
 * Params: x, y, z (block position)
 */
public final class BrewOpenStandAction implements ActionExecutor {

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
        if (!(level.getBlockState(pos).getBlock() instanceof BrewingStandBlock)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "No brewing stand at (" + x + "," + y + "," + z + ")");
        }

        Vec3 center = Vec3.atCenterOf(pos);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, center);

        Vec3 cursor = new Vec3(x + 0.5, y + 0.5, z + 0.5);
        BlockHitResult hit = new BlockHitResult(cursor, Direction.UP, pos, false);
        InteractionResult result = gm.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        JsonObject data = new JsonObject();
        data.addProperty("opened", result.consumesAction());
        return ActionResult.ok(data);
    }
}
