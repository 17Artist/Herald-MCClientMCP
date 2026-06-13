package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Port of BlackBoxPro advanced/RecipeBookSeenAction.kt. 1.20.1's packet takes
 * a {@link Recipe} object, so we look up the recipe by id from the client
 * recipe manager and reject unknown ids.
 */
public final class RecipeBookSeenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String recipeId = JsonUtil.requireString(params, "recipeId");
        ResourceLocation rl = ResourceLocation.tryParse(recipeId);
        if (rl == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid recipe id: " + recipeId);
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        RecipeManager rm = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.getRecipeManager()
            : null;
        Recipe<?> recipe = rm != null ? rm.byKey(rl).orElse(null) : null;
        if (recipe == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown recipe: " + recipeId);
        conn.send(new ServerboundRecipeBookSeenRecipePacket(recipe));
        return ActionResult.ok();
    }
}
