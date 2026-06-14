package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class QueryEntityDetailAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientLevel level = McHelper.level();
        if (player == null || level == null) return McHelper.notInGame();

        int entityId = JsonUtil.requireInt(params, "entityId");

        Entity entity = null;
        for (Entity e : level.entitiesForRendering()) {
            if (e.getId() == entityId) {
                entity = e;
                break;
            }
        }

        if (entity == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Entity not found with id: " + entityId);
        }

        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        JsonObject data = new JsonObject();
        data.addProperty("id", entity.getId());
        data.addProperty("uuid", entity.getStringUUID());
        data.addProperty("type", typeId != null ? typeId.toString() : "unknown");

        // Position
        data.addProperty("x", entity.getX());
        data.addProperty("y", entity.getY());
        data.addProperty("z", entity.getZ());
        data.addProperty("yaw", entity.getYRot());
        data.addProperty("pitch", entity.getXRot());

        // Motion
        Vec3 delta = entity.getDeltaMovement();
        JsonObject motion = new JsonObject();
        motion.addProperty("x", delta.x);
        motion.addProperty("y", delta.y);
        motion.addProperty("z", delta.z);
        data.add("motion", motion);

        // Bounding box
        AABB bb = entity.getBoundingBox();
        JsonObject box = new JsonObject();
        box.addProperty("minX", bb.minX);
        box.addProperty("minY", bb.minY);
        box.addProperty("minZ", bb.minZ);
        box.addProperty("maxX", bb.maxX);
        box.addProperty("maxY", bb.maxY);
        box.addProperty("maxZ", bb.maxZ);
        data.add("boundingBox", box);

        // Custom name
        if (entity.hasCustomName()) {
            data.addProperty("customName", entity.getCustomName().getString());
        }

        // Tags
        Set<String> tags = entity.getTags();
        if (!tags.isEmpty()) {
            JsonArray tagArr = new JsonArray();
            for (String t : tags) tagArr.add(t);
            data.add("tags", tagArr);
        }

        data.addProperty("onGround", entity.onGround());

        // LivingEntity specifics
        if (entity instanceof LivingEntity le) {
            data.addProperty("health", le.getHealth());
            data.addProperty("maxHealth", le.getMaxHealth());
            data.addProperty("armorValue", le.getArmorValue());
            data.addProperty("absorption", le.getAbsorptionAmount());

            // Equipment
            JsonObject equipment = new JsonObject();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = le.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    equipment.addProperty(slot.getName(),
                        (itemId != null ? itemId.toString() : "unknown") + " x" + stack.getCount());
                }
            }
            if (equipment.size() > 0) {
                data.add("equipment", equipment);
            }
        }

        return ActionResult.ok(data);
    }
}
