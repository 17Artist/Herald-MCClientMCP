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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Async: places blocks from a schematic data array, one per tick via /setblock.
 * Params: data (JsonArray of {x,y,z,block}), originX, originY, originZ
 */
public final class BuildSchematicAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        JsonArray data = JsonUtil.getArrayOrEmpty(params, "data");
        if (data.isEmpty()) {
            JsonObject result = new JsonObject();
            result.addProperty("blocks_placed", 0);
            return ActionResult.ok(result);
        }
        int originX = JsonUtil.getIntOrDefault(params, "originX", 0);
        int originY = JsonUtil.getIntOrDefault(params, "originY", 0);
        int originZ = JsonUtil.getIntOrDefault(params, "originZ", 0);

        List<int[]> positions = new ArrayList<>();
        List<String> blocks = new ArrayList<>();
        for (JsonElement el : data) {
            if (!el.isJsonObject()) continue;
            JsonObject entry = el.getAsJsonObject();
            int x = JsonUtil.requireInt(entry, "x") + originX;
            int y = JsonUtil.requireInt(entry, "y") + originY;
            int z = JsonUtil.requireInt(entry, "z") + originZ;
            String block = JsonUtil.requireString(entry, "block");
            positions.add(new int[]{x, y, z});
            blocks.add(block);
        }

        if (positions.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No valid entries in data");
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("build_schematic", params);

        HeraldClientMod.tickScheduler().schedule(1, new SchematicPlacer(task.taskId(), positions, blocks));
        return ActionResult.async(task.taskId());
    }

    private static final class SchematicPlacer implements Runnable {
        private final String taskId;
        private final List<int[]> positions;
        private final List<String> blocks;
        private int index = 0;

        SchematicPlacer(String taskId, List<int[]> positions, List<String> blocks) {
            this.taskId = taskId;
            this.positions = positions;
            this.blocks = blocks;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { engine.fail(taskId, "Player left world"); return; }

            int[] pos = positions.get(index);
            String block = blocks.get(index);
            p.connection.sendCommand("setblock " + pos[0] + " " + pos[1] + " " + pos[2] + " " + block);
            index++;

            if (index >= positions.size()) {
                JsonObject result = new JsonObject();
                result.addProperty("blocks_placed", positions.size());
                engine.complete(taskId, result);
            } else {
                HeraldClientMod.tickScheduler().schedule(1, this);
            }
        }
    }
}
