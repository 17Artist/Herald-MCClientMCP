package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

/**
 * Lists all mob effects from the built-in registry.
 */
public final class RegistryListEffectsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        JsonArray arr = new JsonArray();

        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("category", effect.getCategory().name().toLowerCase());
            entry.addProperty("color", effect.getColor());
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("effects", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
