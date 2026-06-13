package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Async: starting from target block, BFS flood-fill all connected same-type blocks
 * and remove them via /setblock x y z air, one per tick.
 */
public final class MineVeinAction implements ActionExecutor {

    private static final int DEFAULT_MAX_BLOCKS = 32;

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        int maxBlocks = JsonUtil.getIntOrDefault(params, "maxBlocks", DEFAULT_MAX_BLOCKS);

        BlockPos origin = new BlockPos(x, y, z);
        BlockState originState = level.getBlockState(origin);
        if (originState.isAir()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Target block is air");
        }
        Block targetBlock = originState.getBlock();

        // BFS flood fill to find connected same-type blocks
        List<BlockPos> toMine = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && toMine.size() < maxBlocks) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);
            if (currentState.getBlock() != targetBlock) continue;

            toMine.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            if (level.getBlockState(neighbor).getBlock() == targetBlock) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (toMine.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No matching blocks found");
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(targetBlock);
        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("mine_vein", params);

        HeraldClientMod.tickScheduler().schedule(1, new VeinMiner(task.taskId(), toMine));

        JsonObject data = new JsonObject();
        data.addProperty("taskId", task.taskId());
        data.addProperty("blockType", blockId != null ? blockId.toString() : "unknown");
        data.addProperty("blocksFound", toMine.size());
        return ActionResult.async(task.taskId());
    }

    private static final class VeinMiner implements Runnable {
        private final String taskId;
        private final List<BlockPos> positions;
        private int index = 0;

        VeinMiner(String taskId, List<BlockPos> positions) {
            this.taskId = taskId;
            this.positions = positions;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { engine.fail(taskId, "Player left world"); return; }

            BlockPos pos = positions.get(index);
            p.connection.sendCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air");
            index++;

            if (index >= positions.size()) {
                JsonObject result = new JsonObject();
                result.addProperty("blocks_mined", positions.size());
                engine.complete(taskId, result);
            } else {
                HeraldClientMod.tickScheduler().schedule(1, this);
            }
        }
    }
}
