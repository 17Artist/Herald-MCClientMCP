package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

/** Port of BlackBoxPro advanced/AdvancementTabAction.kt. */
public final class AdvancementTabAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String actionStr = JsonUtil.requireString(params, "action").toLowerCase();
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();

        switch (actionStr) {
            case "open": {
                String tabId = JsonUtil.requireString(params, "tabId");
                ResourceLocation rl = ResourceLocation.tryParse(tabId);
                if (rl == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid identifier: " + tabId);
                Object advObj = conn.getAdvancements().getAdvancements().get(rl);
                if (advObj == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Advancement not found: " + tabId);
                try {
                    ServerboundSeenAdvancementsPacket packet = null;
                    for (Method m : ServerboundSeenAdvancementsPacket.class.getMethods()) {
                        if ("openedTab".equals(m.getName()) && m.getParameterCount() == 1) {
                            packet = (ServerboundSeenAdvancementsPacket) m.invoke(null, advObj);
                            break;
                        }
                    }
                    if (packet == null) {
                        Object inner = McVersionCompat.getAdvancementValue(advObj);
                        packet = ServerboundSeenAdvancementsPacket.openedTab(
                                (net.minecraft.advancements.Advancement) inner);
                    }
                    conn.send(packet);
                } catch (Exception e) {
                    return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to open advancement tab: " + e.getMessage());
                }
                return ActionResult.ok();
            }
            case "close":
                conn.send(ServerboundSeenAdvancementsPacket.closedScreen());
                return ActionResult.ok();
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown action: " + actionStr);
        }
    }
}
