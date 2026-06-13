package ai.herald.clientmod.action.scan;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * scan_column — 扫描单列方块（指定 x,z 的一整列），返回非空气方块列表。
 */
public final class ScanColumnAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int z = JsonUtil.requireInt(params, "z");
        int yMin = JsonUtil.getIntOrDefault(params, "yMin", McVersionCompat.getMinBuildHeight(level));
        int yMax = JsonUtil.getIntOrDefault(params, "yMax", McVersionCompat.getMaxBuildHeight(level) - 1);

        JsonArray blocks = new JsonArray();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = yMin; y <= yMax; y++) {
            mutable.set(x, y, z);
            BlockState state = level.getBlockState(mutable);
            if (!state.isAir()) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                JsonObject entry = new JsonObject();
                entry.addProperty("y", y);
                entry.addProperty("block", id != null ? id.toString() : "unknown");
                blocks.add(entry);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("x", x);
        data.addProperty("z", z);
        data.addProperty("yMin", yMin);
        data.addProperty("yMax", yMax);
        data.add("blocks", blocks);
        data.addProperty("count", blocks.size());
        return ActionResult.ok(data);
    }
}
