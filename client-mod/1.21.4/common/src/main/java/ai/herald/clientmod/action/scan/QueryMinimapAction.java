package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * query_minimap — 以玩家为中心，返回指定 Y 层的 2D 方块地图（palette 压缩）。
 */
public final class QueryMinimapAction implements ActionExecutor {

    private static final int MAX_RADIUS = 64;

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int radius = JsonUtil.getIntOrDefault(params, "radius", 16);
        if (radius > MAX_RADIUS) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Radius " + radius + " exceeds max " + MAX_RADIUS);
        }

        int yLevel = JsonUtil.getIntOrDefault(params, "yLevel", player.blockPosition().getY());
        int cx = player.blockPosition().getX();
        int cz = player.blockPosition().getZ();

        int x1 = cx - radius;
        int z1 = cz - radius;
        int x2 = cx + radius;
        int z2 = cz + radius;
        int side = 2 * radius + 1;

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteMap = new HashMap<>();
        JsonArray blocks = new JsonArray();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int z = z1; z <= z2; z++) {
            for (int x = x1; x <= x2; x++) {
                mutable.set(x, yLevel, z);
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
        data.addProperty("sideLength", side);
        data.addProperty("yLevel", yLevel);
        data.addProperty("centerX", cx);
        data.addProperty("centerZ", cz);
        return ActionResult.ok(data);
    }
}
