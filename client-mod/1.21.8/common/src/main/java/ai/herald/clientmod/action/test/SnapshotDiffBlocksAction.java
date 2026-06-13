package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.SnapshotManager;
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
 * snapshot_diff_blocks — compares a stored area snapshot with current blocks.
 * Params: snapshotName(string), x1, y1, z1, x2, y2, z2
 */
public final class SnapshotDiffBlocksAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        String snapshotName = JsonUtil.requireString(params, "snapshotName");
        int x1 = JsonUtil.requireInt(params, "x1");
        int y1 = JsonUtil.requireInt(params, "y1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int y2 = JsonUtil.requireInt(params, "y2");
        int z2 = JsonUtil.requireInt(params, "z2");

        JsonObject snapshot = SnapshotManager.get(snapshotName);
        if (snapshot == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Snapshot not found: " + snapshotName);
        }

        if (!snapshot.has("palette") || !snapshot.has("blocks")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Snapshot is not an area snapshot (missing palette/blocks)");
        }

        // Extract palette and blocks from snapshot
        JsonArray paletteArr = snapshot.getAsJsonArray("palette");
        JsonArray blocksArr = snapshot.getAsJsonArray("blocks");

        // Get snapshot origin and size
        JsonObject origin = snapshot.getAsJsonObject("origin");
        JsonObject size = snapshot.getAsJsonObject("size");
        int ox = origin.get("x").getAsInt();
        int oy = origin.get("y").getAsInt();
        int oz = origin.get("z").getAsInt();
        int sx = size.get("x").getAsInt();
        int sy = size.get("y").getAsInt();
        int sz = size.get("z").getAsInt();

        // Compare current blocks in the specified area with the snapshot
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        JsonArray changes = new JsonArray();
        int totalChanged = 0;
        int totalUnchanged = 0;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    // Calculate index into snapshot data
                    int relX = x - ox;
                    int relY = y - oy;
                    int relZ = z - oz;

                    // Skip if outside snapshot bounds
                    if (relX < 0 || relX >= sx || relY < 0 || relY >= sy || relZ < 0 || relZ >= sz) {
                        continue;
                    }

                    int index = (relY * sz + relZ) * sx + relX;
                    if (index < 0 || index >= blocksArr.size()) continue;

                    int paletteIdx = blocksArr.get(index).getAsInt();
                    String snapshotBlock = paletteArr.get(paletteIdx).getAsString();

                    // Get current block
                    mutable.set(x, y, z);
                    BlockState currentState = level.getBlockState(mutable);
                    ResourceLocation currentId = BuiltInRegistries.BLOCK.getKey(currentState.getBlock());
                    String currentBlock = currentId != null ? currentId.toString() : "minecraft:air";

                    if (!snapshotBlock.equals(currentBlock)) {
                        totalChanged++;
                        if (changes.size() < 1000) { // Cap changes array
                            JsonObject change = new JsonObject();
                            change.addProperty("x", x);
                            change.addProperty("y", y);
                            change.addProperty("z", z);
                            change.addProperty("was", snapshotBlock);
                            change.addProperty("now", currentBlock);
                            changes.add(change);
                        }
                    } else {
                        totalUnchanged++;
                    }
                }
            }
        }

        JsonObject data = new JsonObject();
        data.add("changes", changes);
        data.addProperty("totalChanged", totalChanged);
        data.addProperty("totalUnchanged", totalUnchanged);
        return ActionResult.ok(data);
    }
}
