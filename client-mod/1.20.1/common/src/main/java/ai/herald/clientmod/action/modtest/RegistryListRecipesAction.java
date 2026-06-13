package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Lists recipe IDs, filtered by type and/or namespace.
 */
public final class RegistryListRecipesAction implements ActionExecutor {

    private static final int LIMIT = 500;

    @Override
    public ActionResult execute(JsonObject params) {
        ClientPacketListener connection = McHelper.connection();
        if (connection == null) return McHelper.notConnected();

        String typeFilter = JsonUtil.getStringOrDefault(params, "type", null);
        String namespace = JsonUtil.getStringOrDefault(params, "namespace", null);
        String typeFilterLower = typeFilter != null ? typeFilter.toLowerCase() : null;

        RecipeManager recipeManager = (net.minecraft.world.item.crafting.RecipeManager) McVersionCompat.getRecipeManager(connection);

        JsonArray arr = new JsonArray();
        for (Object recipeObj : McVersionCompat.iterateRecipes(recipeManager)) {
            if (arr.size() >= LIMIT) break;
            ResourceLocation id = McVersionCompat.getRecipeId(recipeObj);
            if (id == null) continue;
            Recipe<?> recipe = (Recipe<?>) McVersionCompat.getRecipeValue(recipeObj);
            String recipeType = recipe.getType().toString();

            if (namespace != null && !id.getNamespace().equals(namespace)) continue;
            if (typeFilterLower != null && !recipeType.toLowerCase().contains(typeFilterLower)) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("type", recipeType);
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("recipes", arr);
        data.addProperty("count", arr.size());
        data.addProperty("limited", arr.size() >= LIMIT);
        return ActionResult.ok(data);
    }
}
