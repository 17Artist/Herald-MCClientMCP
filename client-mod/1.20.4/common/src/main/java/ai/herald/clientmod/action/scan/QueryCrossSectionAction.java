package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * query_cross_section — 返回指定 Y 层、指定 xz 范围的 2D 方块横截面（palette 压缩）。
 * 最大面积 64×64。
 */
public final class QueryCrossSectionAction implements ActionExecutor {

    private static final int MAX_SIDE = 64;

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int y = JsonUtil.requireInt(params, "y");
        int x1 = JsonUtil.requireInt(params, "x1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int z2 = JsonUtil.requireInt(params, "z2");

        int dx = Math.abs(x2 - x1) + 1;
        int dz = Math.abs(z2 - z1) + 1;
        long area = (long) dx * dz;

        if (area > (long) MAX_SIDE * MAX_SIDE) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Area " + area + " exceeds max " + (MAX_SIDE * MAX_SIDE));
        }

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteMap = new HashMap<>();
        JsonArray blocks = new JsonArray();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                mutable.set(x, y, z);
                BlockState state = level.getBlockState(mutable);
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                String blockId = id != null ? id.toString() : "minecraft:air";

                int idx = paletteMap.computeIfAbsent(blockId, k -> {
                    palette.add(k);
                    return palette.size() - 1;
                });
                blocks.add(idx);
            }
        }

        JsonObject data = new JsonObject();
        JsonArray paletteArr = new JsonArray();
        palette.forEach(paletteArr::add);
        data.add("palette", paletteArr);
        data.add("blocks", blocks);

        JsonObject size = new JsonObject();
        size.addProperty("x", dx);
        size.addProperty("z", dz);
        data.add("size", size);

        data.addProperty("y", y);
        data.addProperty("x1", minX);
        data.addProperty("z1", minZ);
        data.addProperty("x2", maxX);
        data.addProperty("z2", maxZ);
        return ActionResult.ok(data);
    }
}
