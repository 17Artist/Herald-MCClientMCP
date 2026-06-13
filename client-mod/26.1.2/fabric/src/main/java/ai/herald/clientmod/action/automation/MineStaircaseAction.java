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
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Async: mines a staircase going downward in the specified direction.
 * Each step is a 1x2x1 area, descending by 1 block per step.
 * Uses multiple /fill commands dispatched one per tick.
 */
public final class MineStaircaseAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String direction = JsonUtil.requireString(params, "direction").toLowerCase();
        int depth = JsonUtil.requireInt(params, "depth");

        if (depth < 1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "depth must be >= 1");
        }

        int dx = 0, dz = 0;
        switch (direction) {
            case "north": dz = -1; break;
            case "south": dz = 1; break;
            case "east": dx = 1; break;
            case "west": dx = -1; break;
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "direction must be north/south/east/west, got: " + direction);
        }

        // Build list of fill commands for each stair step
        // Each step: clear a 1-wide, 2-high space and go down 1
        List<String> commands = new ArrayList<>();
        int cx = x, cy = y, cz = z;
        for (int i = 0; i < depth; i++) {
            // Clear 1x3x1 area (standing room + block below feet removed)
            commands.add("fill " + cx + " " + (cy - 1) + " " + cz + " " + cx + " " + (cy + 1) + " " + cz + " air");
            cx += dx;
            cz += dz;
            cy -= 1;
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("mine_staircase", params);

        HeraldClientMod.tickScheduler().schedule(1, new StaircaseMiner(task.taskId(), commands));

        JsonObject data = new JsonObject();
        data.addProperty("taskId", task.taskId());
        data.addProperty("steps", depth);
        data.addProperty("direction", direction);
        return ActionResult.async(task.taskId());
    }

    private static final class StaircaseMiner implements Runnable {
        private final String taskId;
        private final List<String> commands;
        private int index = 0;

        StaircaseMiner(String taskId, List<String> commands) {
            this.taskId = taskId;
            this.commands = commands;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { engine.fail(taskId, "Player left world"); return; }

            p.connection.sendCommand(commands.get(index));
            index++;

            if (index >= commands.size()) {
                JsonObject result = new JsonObject();
                result.addProperty("steps_mined", commands.size());
                engine.complete(taskId, result);
            } else {
                HeraldClientMod.tickScheduler().schedule(1, this);
            }
        }
    }
}
