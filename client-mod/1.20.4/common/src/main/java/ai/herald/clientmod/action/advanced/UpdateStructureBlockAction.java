package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

/** Port of BlackBoxPro advanced/UpdateStructureBlockAction.kt. */
public final class UpdateStructureBlockAction implements ActionExecutor {

    private static final int IGNORE_ENTITIES_FLAG = 0x01;
    private static final int SHOW_AIR_FLAG = 0x02;
    private static final int SHOW_BOUNDING_BOX_FLAG = 0x04;

    @Override
    public ActionResult execute(JsonObject params) {
        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        int actionInt = JsonUtil.requireInt(params, "action");
        String modeStr = JsonUtil.requireString(params, "mode").toLowerCase();
        String name = JsonUtil.requireString(params, "name");
        int offX = JsonUtil.getIntOrDefault(params, "offsetX", 0);
        int offY = JsonUtil.getIntOrDefault(params, "offsetY", 0);
        int offZ = JsonUtil.getIntOrDefault(params, "offsetZ", 0);
        int sizeX = JsonUtil.getIntOrDefault(params, "sizeX", 0);
        int sizeY = JsonUtil.getIntOrDefault(params, "sizeY", 0);
        int sizeZ = JsonUtil.getIntOrDefault(params, "sizeZ", 0);
        String mirrorStr = JsonUtil.getStringOrDefault(params, "mirror", "none").toLowerCase();
        String rotationStr = JsonUtil.getStringOrDefault(params, "rotation", "none").toLowerCase();
        String metadata = JsonUtil.getStringOrDefault(params, "metadata", "");
        float integrity = (float) JsonUtil.getDoubleOrDefault(params, "integrity", 1.0);
        long seed = JsonUtil.getLongOrDefault(params, "seed", 0L);
        int flags = JsonUtil.getIntOrDefault(params, "flags", 0);

        StructureBlockEntity.UpdateType action;
        switch (actionInt) {
            case 0: action = StructureBlockEntity.UpdateType.UPDATE_DATA; break;
            case 1: action = StructureBlockEntity.UpdateType.SAVE_AREA; break;
            case 2: action = StructureBlockEntity.UpdateType.LOAD_AREA; break;
            case 3: action = StructureBlockEntity.UpdateType.SCAN_AREA; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown action: " + actionInt);
        }
        StructureMode mode;
        switch (modeStr) {
            case "save":   mode = StructureMode.SAVE; break;
            case "load":   mode = StructureMode.LOAD; break;
            case "corner": mode = StructureMode.CORNER; break;
            case "data":   mode = StructureMode.DATA; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown mode: " + modeStr);
        }
        Mirror mirror;
        switch (mirrorStr) {
            case "none":       mirror = Mirror.NONE; break;
            case "left_right": mirror = Mirror.LEFT_RIGHT; break;
            case "front_back": mirror = Mirror.FRONT_BACK; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown mirror: " + mirrorStr);
        }
        Rotation rotation;
        switch (rotationStr) {
            case "none":                 rotation = Rotation.NONE; break;
            case "clockwise_90":         rotation = Rotation.CLOCKWISE_90; break;
            case "clockwise_180":        rotation = Rotation.CLOCKWISE_180; break;
            case "counterclockwise_90":  rotation = Rotation.COUNTERCLOCKWISE_90; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown rotation: " + rotationStr);
        }

        boolean ignoreEntities = (flags & IGNORE_ENTITIES_FLAG) != 0;
        boolean showAir = (flags & SHOW_AIR_FLAG) != 0;
        boolean showBoundingBox = (flags & SHOW_BOUNDING_BOX_FLAG) != 0;

        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        // Use reflection for cross-version compat
        try { var ctors = net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket.class.getConstructors(); for (var ctor : ctors) { if (ctor.getParameterCount() >= 14) { conn.send((net.minecraft.network.protocol.Packet<?>) ctor.newInstance(
            new BlockPos(x, y, z), action, mode, name,
            new BlockPos(offX, offY, offZ), new Vec3i(sizeX, sizeY, sizeZ),
            mirror, rotation, metadata,
            ignoreEntities, showAir, showBoundingBox, integrity, seed)); break; } } } catch (Exception ignored) {}
        return ActionResult.ok();
    }
}
