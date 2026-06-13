package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;


/**
 * Port of BlackBoxPro advanced/SetBeaconEffectAction.kt. 1.20.1's packet takes
 * {@code Optional<MobEffect>}; we resolve raw numeric ids via
 * {@link BuiltInRegistries#MOB_EFFECT}. A negative id (or omitted) means "no
 * effect selected".
 */
public final class SetBeaconEffectAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int primary = JsonUtil.getIntOrDefault(params, "primaryEffect", -1);
        int secondary = JsonUtil.getIntOrDefault(params, "secondaryEffect", -1);
        if (primary >= 0 && resolveMobEffectById(primary) == null)
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown primaryEffect id: " + primary);
        if (secondary >= 0 && resolveMobEffectById(secondary) == null)
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown secondaryEffect id: " + secondary);
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        McVersionCompat.sendSetBeaconPacket(conn, primary, secondary);
        return ActionResult.ok();
    }

    private static Object resolveMobEffectById(int id) {
        if (id < 0) return null;
        try {
            return BuiltInRegistries.MOB_EFFECT.byId(id);
        } catch (Exception e) {
            return null;
        }
    }
}
