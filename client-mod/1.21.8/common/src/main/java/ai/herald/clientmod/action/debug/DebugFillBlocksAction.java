package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DebugFillBlocksAction implements ActionExecutor {

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

        Minecraft mc = McHelper.mc();

        // In singleplayer: directly set blocks on server level (bypasses cheats check)
        if (mc.getSingleplayerServer() != null) {
            ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(
                mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID()).level().dimension()
            );
            if (serverLevel == null) {
                return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Cannot access server level");
            }

            // Parse block
            String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
            Block block = McVersionCompat.registryGet(BuiltInRegistries.BLOCK, ResourceLocation.tryParse(fullId));
            if (block == null || block == McVersionCompat.<net.minecraft.world.level.block.Block>registryGet(BuiltInRegistries.BLOCK, ResourceLocation.tryParse("minecraft:air")) && !fullId.equals("minecraft:air")) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown block: " + blockId);
            }
            BlockState state = block.defaultBlockState();

            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

            int count = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        serverLevel.setBlock(pos, state, 3);
                        count++;
                    }
                }
            }

            JsonObject data = new JsonObject();
            data.addProperty("blocksChanged", count);
            data.addProperty("blockId", fullId);
            return ActionResult.ok(data);
        } else {
            // Multiplayer: fallback to command (requires OP)
            String cmd = "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + blockId;
            player.connection.sendCommand(cmd);
            JsonObject data = new JsonObject();
            data.addProperty("command", cmd);
            return ActionResult.ok(data);
        }
    }
}
