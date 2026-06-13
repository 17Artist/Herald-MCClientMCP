package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Sync: opens the villager trade screen by interacting with the villager entity.
 */
public final class TradeOpenVillagerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");

        ClientLevel level = McHelper.level();
        ClientPacketListener conn = McHelper.connection();
        if (level == null || conn == null) return McHelper.notInGame();

        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }
        if (!(entity instanceof Villager)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Entity " + entityId + " is not a villager");
        }

        conn.send(new ServerboundInteractPacket(entity.getId(), InteractionHand.MAIN_HAND, null, false));

        JsonObject data = new JsonObject();
        data.addProperty("entityId", entityId);
        data.addProperty("interacted", true);
        return ActionResult.ok(data);
    }
}
