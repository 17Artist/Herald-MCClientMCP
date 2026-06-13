package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Sync: calculates flee direction (away from nearest hostile), sets sprint and movement.
 */
public final class CombatFleeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        double distance = JsonUtil.getDoubleOrDefault(params, "distance", 8.0);
        if (distance <= 0) distance = 8.0;

        // Find nearest hostile
        AABB box = player.getBoundingBox().inflate(16.0);
        List<Entity> entities = level.getEntities(player, box);
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            if (!(e instanceof Monster)) continue;
            double d = e.distanceToSqr(player);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = e;
            }
        }

        float fleeYaw;
        if (nearest != null) {
            // Run away from the hostile
            double dx = player.getX() - nearest.getX();
            double dz = player.getZ() - nearest.getZ();
            fleeYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        } else {
            // No hostile found, just run backwards
            fleeYaw = player.getYRot() + 180f;
        }

        // Set look direction and sprint
        player.setYRot(fleeYaw);
        player.setSprinting(true);

        // Apply impulse in flee direction
        double rad = Math.toRadians(fleeYaw);
        double speed = Math.min(distance * 0.3, 2.0);
        double vx = -Math.sin(rad) * speed;
        double vz = Math.cos(rad) * speed;
        player.setDeltaMovement(vx, player.getDeltaMovement().y, vz);
        player.hurtMarked = true;

        JsonObject data = new JsonObject();
        data.addProperty("flee_yaw", fleeYaw);
        data.addProperty("hostile_found", nearest != null);
        if (nearest != null) {
            data.addProperty("threat_distance", Math.sqrt(nearestDist));
        }
        return ActionResult.ok(data);
    }
}
