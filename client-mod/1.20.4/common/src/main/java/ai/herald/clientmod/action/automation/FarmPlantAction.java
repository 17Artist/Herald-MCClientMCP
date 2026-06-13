package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: plants a crop at the given position.
 * Checks that block below is farmland, then sets the crop block at age=0.
 */
public final class FarmPlantAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String seedItem = JsonUtil.requireString(params, "seedItem");

        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        if (belowState.getBlock() != Blocks.FARMLAND) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Block below is not farmland");
        }

        String cropBlock = seedToCrop(seedItem);
        if (cropBlock == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Unknown seed item: " + seedItem);
        }

        player.connection.sendCommand("setblock " + x + " " + y + " " + z + " " + cropBlock + "[age=0]");

        JsonObject data = new JsonObject();
        data.addProperty("planted", cropBlock);
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        return ActionResult.ok(data);
    }

    static String seedToCrop(String seedItem) {
        if (seedItem == null) return null;
        switch (seedItem.toLowerCase().replace("minecraft:", "")) {
            case "wheat_seeds": return "wheat";
            case "carrot": case "carrots": return "carrots";
            case "potato": case "potatoes": return "potatoes";
            case "beetroot_seeds": return "beetroots";
            case "melon_seeds": return "melon_stem";
            case "pumpkin_seeds": return "pumpkin_stem";
            case "torchflower_seeds": return "torchflower_crop";
            default: return null;
        }
    }
}
