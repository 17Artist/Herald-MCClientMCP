package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Sync: queries detailed info about a villager entity.
 * Returns profession, level, xp, offer count, and villager type.
 */
public final class QueryVillagerInfoAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int entityId = JsonUtil.requireInt(params, "entityId");

        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No entity with id " + entityId);
        }
        if (!(entity instanceof Villager villager)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Entity " + entityId + " is not a villager");
        }

        VillagerData villagerData = villager.getVillagerData();
        Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerData.profession().value());
        Identifier typeId = BuiltInRegistries.VILLAGER_TYPE.getKey(villagerData.type().value());

        MerchantOffers offers = villager.getOffers();

        JsonObject data = new JsonObject();
        data.addProperty("entityId", entityId);
        data.addProperty("profession", professionId != null ? professionId.toString() : "unknown");
        data.addProperty("level", villagerData.level());
        data.addProperty("villagerType", typeId != null ? typeId.toString() : "unknown");
        data.addProperty("xp", villager.getVillagerXp());
        data.addProperty("offerCount", offers != null ? offers.size() : 0);
        data.addProperty("isBaby", villager.isBaby());
        data.addProperty("x", villager.getX());
        data.addProperty("y", villager.getY());
        data.addProperty("z", villager.getZ());
        return ActionResult.ok(data);
    }
}
