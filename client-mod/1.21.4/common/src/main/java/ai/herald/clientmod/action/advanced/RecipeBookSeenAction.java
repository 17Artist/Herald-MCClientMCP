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
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/**
 * Marks a recipe as "seen" in the recipe book.
 * In 1.21.4, the packet takes a {@link RecipeDisplayId} (an integer index)
 * rather than a RecipeHolder. Callers must provide a numeric {@code displayIndex}.
 */
public final class RecipeBookSeenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int displayIndex = JsonUtil.getIntOrDefault(params, "displayIndex", -1);
        if (displayIndex < 0) {
            // Legacy recipeId parameter - cannot resolve in 1.21.4 client
            String recipeId = JsonUtil.getStringOrDefault(params, "recipeId", null);
            if (recipeId != null && !recipeId.isEmpty()) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Recipe lookup by id is not supported in 1.21.4; provide displayIndex instead");
            }
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required parameter: displayIndex");
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundRecipeBookSeenRecipePacket(new RecipeDisplayId(displayIndex)));
        return ActionResult.ok();
    }
}
