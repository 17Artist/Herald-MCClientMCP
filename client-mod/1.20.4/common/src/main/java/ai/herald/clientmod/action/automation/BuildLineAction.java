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
 * Async: places blocks along a 3D Bresenham line, one per tick via /setblock.
 */
public final class BuildLineAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x1 = JsonUtil.requireInt(params, "x1");
        int y1 = JsonUtil.requireInt(params, "y1");
        int z1 = JsonUtil.requireInt(params, "z1");
        int x2 = JsonUtil.requireInt(params, "x2");
        int y2 = JsonUtil.requireInt(params, "y2");
        int z2 = JsonUtil.requireInt(params, "z2");
        String blockId = JsonUtil.requireString(params, "blockId");

        List<int[]> positions = bresenham3D(x1, y1, z1, x2, y2, z2);
        if (positions.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Line has no positions");
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("build_line", params);

        HeraldClientMod.tickScheduler().schedule(1, new LinePlacer(task.taskId(), positions, blockId, 0));
        return ActionResult.async(task.taskId());
    }

    private static final class LinePlacer implements Runnable {
        private final String taskId;
        private final List<int[]> positions;
        private final String blockId;
        private int index;

        LinePlacer(String taskId, List<int[]> positions, String blockId, int index) {
            this.taskId = taskId;
            this.positions = positions;
            this.blockId = blockId;
            this.index = index;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { engine.fail(taskId, "Player left world"); return; }

            int[] pos = positions.get(index);
            p.connection.sendCommand("setblock " + pos[0] + " " + pos[1] + " " + pos[2] + " " + blockId);
            index++;

            if (index >= positions.size()) {
                JsonObject data = new JsonObject();
                data.addProperty("blocks_placed", positions.size());
                engine.complete(taskId, data);
            } else {
                HeraldClientMod.tickScheduler().schedule(1, this);
            }
        }
    }

    private static List<int[]> bresenham3D(int x1, int y1, int z1, int x2, int y2, int z2) {
        List<int[]> points = new ArrayList<>();
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, sz = z1 < z2 ? 1 : -1;
        int max = Math.max(dx, Math.max(dy, dz));
        if (max == 0) { points.add(new int[]{x1, y1, z1}); return points; }

        double ix = (double) dx / max, iy = (double) dy / max, iz = (double) dz / max;
        double ex = 0, ey = 0, ez = 0;
        int cx = x1, cy = y1, cz = z1;
        for (int i = 0; i <= max; i++) {
            points.add(new int[]{cx, cy, cz});
            ex += ix; ey += iy; ez += iz;
            if (ex >= 0.5) { cx += sx; ex -= 1.0; }
            if (ey >= 0.5) { cy += sy; ey -= 1.0; }
            if (ez >= 0.5) { cz += sz; ez -= 1.0; }
        }
        return points;
    }
}
