package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sync: finds an entity by ID, looks at it, and attacks it.
 */
public final class CombatTargetEntityAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || level == null || gm == null) return McHelper.notInGame();

        int entityId = JsonUtil.requireInt(params, "entityId");
        boolean approach = JsonUtil.getBooleanOrDefault(params, "approach", false);

        Entity target = level.getEntity(entityId);
        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }

        if (approach) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "approach=true requires pathfinding (not implemented)");
        }

        // Look at target
        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
        player.setYRot(yaw);
        player.setXRot(pitch);

        // Attack
        gm.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);

        JsonObject data = new JsonObject();
        data.addProperty("entity_id", entityId);
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        data.addProperty("entity_type", typeId != null ? typeId.toString() : "unknown");
        data.addProperty("distance", Math.sqrt(target.distanceToSqr(player)));
        if (target instanceof LivingEntity le) {
            data.addProperty("health", le.getHealth());
        }
        return ActionResult.ok(data);
    }
}
