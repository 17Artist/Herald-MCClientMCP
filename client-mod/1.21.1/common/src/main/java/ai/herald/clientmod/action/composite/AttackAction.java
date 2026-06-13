package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/** Composite "attack target by id" — aim+swing+attack rolled into one. */
public final class AttackAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        int entityId = JsonUtil.requireInt(params, "entityId");
        Entity target = level.getEntity(entityId);
        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }
        // best-effort look — ignore failure, attack still goes through
        new LookAtEntityAction().execute(params);

        gm.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);

        JsonObject data = new JsonObject();
        data.addProperty("entity_id", entityId);
        data.addProperty("entity_type", target.getType().toString());
        return ActionResult.ok(data);
    }
}
