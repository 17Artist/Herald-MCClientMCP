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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sync: scans an area for all crops and returns their growth status.
 */
public final class QueryCropAreaAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        LocalPlayer player = McHelper.player();
        if (level == null || player == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int py = (int) player.getY();

        JsonArray crops = new JsonArray();
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int by = py - 10; by <= py + 10; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock crop)) continue;

                    int age = crop.getAge(state);
                    int maxAge = crop.getMaxAge();
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

                    JsonObject entry = new JsonObject();
                    entry.addProperty("x", bx);
                    entry.addProperty("y", by);
                    entry.addProperty("z", bz);
                    entry.addProperty("crop", blockId != null ? blockId.toString() : "unknown");
                    entry.addProperty("age", age);
                    entry.addProperty("maxAge", maxAge);
                    entry.addProperty("mature", age >= maxAge);
                    crops.add(entry);
                }
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("count", crops.size());
        data.add("crops", crops);
        return ActionResult.ok(data);
    }
}
