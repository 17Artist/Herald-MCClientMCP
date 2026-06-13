package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;

import java.lang.reflect.Constructor;

/**
 * Port of BlackBoxPro advanced/UpdateJigsawBlockAction.kt — packet ctor differs by version.
 */
public final class UpdateJigsawBlockAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String name = JsonUtil.requireString(params, "name");
        String target = JsonUtil.requireString(params, "target");
        String pool = JsonUtil.requireString(params, "pool");
        String finalState = JsonUtil.getStringOrDefault(params, "finalState", "");
        String jointType = JsonUtil.getStringOrDefault(params, "jointType", "rollable").toLowerCase();
        JointType joint;
        switch (jointType) {
            case "rollable": joint = JointType.ROLLABLE; break;
            case "aligned":  joint = JointType.ALIGNED;  break;
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown jointType: " + jointType);
        }
        ResourceLocation nameId = ResourceLocation.tryParse(name);
        ResourceLocation targetId = ResourceLocation.tryParse(target);
        ResourceLocation poolId = ResourceLocation.tryParse(pool);
        if (nameId == null || targetId == null || poolId == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid identifier");
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        BlockPos pos = new BlockPos(x, y, z);
        try {
            Class<?> pktClass = Class.forName("net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket");
            Object packet = null;
            for (Constructor<?> ctor : pktClass.getConstructors()) {
                Class<?>[] types = ctor.getParameterTypes();
                if (types.length == 6 && types[0] == BlockPos.class) {
                    packet = ctor.newInstance(pos, nameId, targetId, poolId, finalState, joint);
                    break;
                }
            }
            if (packet == null) {
                for (Constructor<?> ctor : pktClass.getConstructors()) {
                    if (ctor.getParameterCount() >= 6) {
                        packet = ctor.newInstance(pos, nameId, targetId, poolId, finalState, joint);
                        break;
                    }
                }
            }
            if (packet == null) {
                return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "No compatible ServerboundSetJigsawBlockPacket constructor");
            }
            conn.send((net.minecraft.network.protocol.Packet<?>) packet);
        } catch (Exception e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send jigsaw packet: " + e.getMessage());
        }
        return ActionResult.ok();
    }
}
