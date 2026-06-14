package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: scans an area for mature crops and harvests them.
 * Optionally replants crops at age 0.
 */
public final class HarvestAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");
        boolean replant = JsonUtil.getBooleanOrDefault(params, "replant", false);

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        int harvested = 0;
        JsonArray positions = new JsonArray();

        // Scan Y range where crops typically exist
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int by = -64; by <= 320; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock crop)) continue;
                    if (!crop.isMaxAge(state)) continue;

                    // Harvest
                    player.connection.sendCommand("setblock " + bx + " " + by + " " + bz + " air");
                    harvested++;

                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("x", bx);
                    posObj.addProperty("y", by);
                    posObj.addProperty("z", bz);
                    positions.add(posObj);

                    // Replant
                    if (replant) {
                        String cropName = getCropBlockName(state.getBlock());
                        if (cropName != null) {
                            player.connection.sendCommand(
                                "setblock " + bx + " " + by + " " + bz + " " + cropName + "[age=0]");
                        }
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("harvested", harvested);
        data.add("positions", positions);
        return ActionResult.ok(data);
    }

    private static String getCropBlockName(Block block) {
        if (block == Blocks.WHEAT) return "wheat";
        if (block == Blocks.CARROTS) return "carrots";
        if (block == Blocks.POTATOES) return "potatoes";
        if (block == Blocks.BEETROOTS) return "beetroots";
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null ? id.getPath() : null;
    }
}
