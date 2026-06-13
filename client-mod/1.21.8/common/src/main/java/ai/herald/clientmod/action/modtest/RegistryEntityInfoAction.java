package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Returns detailed information about an entity type from its registry ID.
 */
public final class RegistryEntityInfoAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String entityTypeId = JsonUtil.requireString(params, "entityType");
        ResourceLocation loc = ResourceLocation.tryParse(entityTypeId);
        if (loc == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid entity type ID: " + entityTypeId);
        }

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Entity type not found: " + entityTypeId);
        }

        EntityType<?> type = McVersionCompat.registryGet(BuiltInRegistries.ENTITY_TYPE, loc);

        JsonObject data = new JsonObject();
        data.addProperty("id", loc.toString());

        // Category
        MobCategory category = type.getCategory();
        data.addProperty("category", category.name().toLowerCase());
        data.addProperty("spawnGroup", category.getName());

        // Dimensions
        EntityDimensions dims = type.getDimensions();
        data.addProperty("width", McVersionCompat.entityDimensionsWidth(dims));
        data.addProperty("height", McVersionCompat.entityDimensionsHeight(dims));
        data.addProperty("fixed", McVersionCompat.entityDimensionsFixed(dims));

        // Properties
        data.addProperty("fireImmune", type.fireImmune());
        data.addProperty("canSerialize", type.canSerialize());
        data.addProperty("canSummon", type.canSummon());

        return ActionResult.ok(data);
    }
}
