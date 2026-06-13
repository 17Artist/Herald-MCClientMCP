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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Lists all biomes from the level registry access.
 */
public final class RegistryListBiomesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        JsonArray arr = new JsonArray();

        for (ResourceLocation id : biomeRegistry.keySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("biomes", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
