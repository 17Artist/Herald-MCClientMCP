package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Lists all enchantments from the registry.
 */
public final class RegistryListEnchantmentsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        JsonArray arr = new JsonArray();

        for (Identifier id : registry.keySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("enchantments", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
