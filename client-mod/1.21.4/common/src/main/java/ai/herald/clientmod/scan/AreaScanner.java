package ai.herald.clientmod.scan;

import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 3D 区域方块扫描引擎。
 * 支持 palette 压缩格式（方块ID表 + 索引数组）减少传输量。
 */
public final class AreaScanner {

    /** 最大扫描体积（防止 OOM） */
    public static final int MAX_VOLUME = 32 * 32 * 32; // 32768 blocks

    /**
     * 扫描区域，返回 palette 压缩格式：
     * { "palette": ["minecraft:air", "minecraft:stone", ...],
     *   "blocks": [0,0,1,1,0,...],  // 索引到 palette
     *   "size": { "x": dx, "y": dy, "z": dz },
     *   "origin": { "x": x1, "y": y1, "z": z1 } }
     */
    public static JsonObject scanPalette(ClientLevel level, BlockPos from, BlockPos to) {
        int x1 = Math.min(from.getX(), to.getX());
        int y1 = Math.min(from.getY(), to.getY());
        int z1 = Math.min(from.getZ(), to.getZ());
        int x2 = Math.max(from.getX(), to.getX());
        int y2 = Math.max(from.getY(), to.getY());
        int z2 = Math.max(from.getZ(), to.getZ());

        int dx = x2 - x1 + 1;
        int dy = y2 - y1 + 1;
        int dz = z2 - z1 + 1;

        List<String> palette = new ArrayList<>();
        Map<String, Integer> paletteMap = new HashMap<>();
        JsonArray blocks = new JsonArray();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
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
        }

        JsonObject result = new JsonObject();
        JsonArray paletteArr = new JsonArray();
        palette.forEach(paletteArr::add);
        result.add("palette", paletteArr);
        result.add("blocks", blocks);

        JsonObject size = new JsonObject();
        size.addProperty("x", dx);
        size.addProperty("y", dy);
        size.addProperty("z", dz);
        result.add("size", size);

        JsonObject origin = new JsonObject();
        origin.addProperty("x", x1);
        origin.addProperty("y", y1);
        origin.addProperty("z", z1);
        result.add("origin", origin);

        return result;
    }

    /**
     * 扫描区域，返回稀疏格式（只含非空气方块）：
     * [ { "x":1, "y":64, "z":1, "block":"minecraft:stone" }, ... ]
     */
    public static JsonArray scanSparse(ClientLevel level, BlockPos from, BlockPos to) {
        int x1 = Math.min(from.getX(), to.getX());
        int y1 = Math.min(from.getY(), to.getY());
        int z1 = Math.min(from.getZ(), to.getZ());
        int x2 = Math.max(from.getX(), to.getX());
        int y2 = Math.max(from.getY(), to.getY());
        int z2 = Math.max(from.getZ(), to.getZ());

        JsonArray result = new JsonArray();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    if (!state.isAir()) {
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        JsonObject entry = new JsonObject();
                        entry.addProperty("x", x);
                        entry.addProperty("y", y);
                        entry.addProperty("z", z);
                        entry.addProperty("block", id != null ? id.toString() : "unknown");
                        result.add(entry);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 搜索区域内指定方块类型，返回坐标列表。
     */
    public static JsonArray findBlocks(ClientLevel level, BlockPos center, int radius,
                                       String targetBlockId, int limit) {
        JsonArray result = new JsonArray();
        int found = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dy = -radius; dy <= radius && found < limit; dy++) {
            for (int dx = -radius; dx <= radius && found < limit; dx++) {
                for (int dz = -radius; dz <= radius && found < limit; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(mutable);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (id != null && id.toString().equals(targetBlockId)) {
                        JsonObject entry = new JsonObject();
                        entry.addProperty("x", mutable.getX());
                        entry.addProperty("y", mutable.getY());
                        entry.addProperty("z", mutable.getZ());
                        result.add(entry);
                        found++;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 统计区域内各方块数量。
     */
    public static JsonObject countBlocks(ClientLevel level, BlockPos from, BlockPos to) {
        int x1 = Math.min(from.getX(), to.getX());
        int y1 = Math.min(from.getY(), to.getY());
        int z1 = Math.min(from.getZ(), to.getZ());
        int x2 = Math.max(from.getX(), to.getX());
        int y2 = Math.max(from.getY(), to.getY());
        int z2 = Math.max(from.getZ(), to.getZ());

        Map<String, Integer> counts = new HashMap<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    String blockId = id != null ? id.toString() : "minecraft:air";
                    counts.merge(blockId, 1, Integer::sum);
                }
            }
        }

        JsonObject result = new JsonObject();
        counts.forEach(result::addProperty);
        return result;
    }

    /**
     * 获取地表高度图（每个 xz 位置的最高非空气方块 y 坐标）。
     */
    public static JsonObject surfaceMap(ClientLevel level, int x1, int z1, int x2, int z2) {
        JsonArray rows = new JsonArray();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int z = z1; z <= z2; z++) {
            JsonArray row = new JsonArray();
            for (int x = x1; x <= x2; x++) {
                int topY = McVersionCompat.getMaxBuildHeight(level);
                for (int y = topY - 1; y >= McVersionCompat.getMinBuildHeight(level); y--) {
                    mutable.set(x, y, z);
                    if (!level.getBlockState(mutable).isAir()) {
                        topY = y;
                        break;
                    }
                }
                row.add(topY);
            }
            rows.add(row);
        }

        JsonObject result = new JsonObject();
        result.add("heightmap", rows);
        result.addProperty("x1", x1);
        result.addProperty("z1", z1);
        result.addProperty("x2", x2);
        result.addProperty("z2", z2);
        return result;
    }
}
