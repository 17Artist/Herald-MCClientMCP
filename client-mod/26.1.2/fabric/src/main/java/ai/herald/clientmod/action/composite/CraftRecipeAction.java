package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/**
 * Send a recipe-book "craft" request. In 1.21.4, the packet takes a
 * {@link RecipeDisplayId} (integer index) rather than a RecipeHolder.
 * Callers should provide {@code displayIndex} (the recipe display ID).
 */
public final class CraftRecipeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || level == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int windowId = JsonUtil.getIntOrDefault(params, "windowId", player.containerMenu.containerId);
        boolean shift = JsonUtil.getBooleanOrDefault(params, "makeAll", false);

        int displayIndex = JsonUtil.getIntOrDefault(params, "displayIndex", -1);
        if (displayIndex < 0) {
            // Legacy recipeId parameter — cannot directly map to displayIndex in 1.21.4
            String recipeId = JsonUtil.getStringOrDefault(params, "recipeId", null);
            if (recipeId != null && !recipeId.isEmpty()) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Recipe lookup by id requires displayIndex in 1.21.4; pass displayIndex directly");
            }
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required parameter: displayIndex");
        }

        conn.send(new ServerboundPlaceRecipePacket(windowId, new RecipeDisplayId(displayIndex), shift));

        JsonObject data = new JsonObject();
        data.addProperty("window_id", windowId);
        data.addProperty("display_index", displayIndex);
        data.addProperty("make_all", shift);
        return ActionResult.ok(data);
    }
}
