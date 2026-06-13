package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;

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
                Identifier rl = Identifier.tryParse(tabId);
                if (rl == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid identifier: " + tabId);
                AdvancementNode node = conn.getAdvancements().getTree().get(rl);
                if (node == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Advancement not found: " + tabId);
                AdvancementHolder holder = node.holder();
                conn.send(ServerboundSeenAdvancementsPacket.openedTab(holder));
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
