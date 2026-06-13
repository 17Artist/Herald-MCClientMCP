package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Async multi-tick mine of a single block: aim → start_destroy → wait the
 * estimated hardness ticks → stop_destroy. The skill task completes once
 * the stop packet is sent (or the block went air mid-mine).
 */
public final class BreakBlockAction implements ActionExecutor {

    private static final int DEFAULT_BREAK_TICKS = 20;
    private static final int MAX_BREAK_TICKS = 20 * 30; // 30 seconds upper bound

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || level == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        BlockPos pos = new BlockPos(x, y, z);

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Block is air at (" + x + "," + y + "," + z + ")");
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Block at (" + x + "," + y + "," + z + ") is unbreakable");
        }
        int breakTicks;
        if (hardness == 0f) {
            breakTicks = 1;
        } else {
            float speed = player.getDestroySpeed(state);
            if (speed > 0f) {
                breakTicks = Math.max(1, (int) Math.ceil(hardness * 30.0f / speed));
            } else {
                breakTicks = DEFAULT_BREAK_TICKS;
            }
        }
        if (breakTicks > MAX_BREAK_TICKS) breakTicks = MAX_BREAK_TICKS;

        // Aim at the top face first.
        JsonObject lookParams = new JsonObject();
        lookParams.addProperty("x", x);
        lookParams.addProperty("y", y);
        lookParams.addProperty("z", z);
        lookParams.addProperty("face", "up");
        new LookAtBlockAction().execute(lookParams);

        // dig_start
        conn.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("break_block");
        final int waitTicks = breakTicks;
        HeraldClientMod.tickScheduler().schedule(waitTicks, () -> finish(task.taskId(), pos));

        return ActionResult.async(task.taskId());
    }

    private void finish(String taskId, BlockPos pos) {
        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.get(taskId);
        if (task == null || task.status() != SkillStatus.RUNNING) {
            // already cancelled / failed — best-effort abort to leave server consistent
            tryAbort(pos);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || level == null || conn == null) {
            engine.fail(taskId, "Disconnected mid-break");
            return;
        }
        boolean wasAir = level.getBlockState(pos).isAir();
        ServerboundPlayerActionPacket.Action act = wasAir
            ? ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
            : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
        conn.send(new ServerboundPlayerActionPacket(act, pos, Direction.UP));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

        JsonObject data = new JsonObject();
        data.addProperty("x", pos.getX());
        data.addProperty("y", pos.getY());
        data.addProperty("z", pos.getZ());
        data.addProperty("was_already_air", wasAir);
        engine.complete(taskId, data);
    }

    private void tryAbort(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn != null) {
            conn.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
        }
    }
}
