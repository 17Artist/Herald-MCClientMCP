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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

/**
 * Send a recipe-book "craft" request. The reference (Yarn 1.21.1) keys
 * recipes by index from the recipe book screen; in 1.20.1 the packet still
 * takes a {@link Recipe} instance, so we resolve via {@code recipeId}
 * (a {@link ResourceLocation} string) and fail with INVALID_PARAMS when
 * neither {@code recipeId} nor a resolvable index is given.
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

        String recipeId = JsonUtil.getStringOrDefault(params, "recipeId", null);
        RecipeHolder<?> recipe = null;
        if (recipeId != null && !recipeId.isEmpty()) {
            ResourceLocation loc = ResourceLocation.tryParse(recipeId);
            if (loc == null) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid recipeId: " + recipeId);
            }
            Optional<RecipeHolder<?>> opt = level.getRecipeManager().byKey(loc);
            if (opt.isEmpty()) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Recipe not found: " + recipeId);
            }
            recipe = opt.get();
        }

        if (recipe == null) {
            // Index-based resolution isn't reliably possible in 1.20.1 without
            // poking the screen-side recipe book; surface a clean error.
            if (params != null && params.has("recipeIndex")) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Recipe index not resolvable in 1.20.1 — pass recipeId (ResourceLocation) instead");
            }
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required parameter: recipeId");
        }

        conn.send(new ServerboundPlaceRecipePacket(windowId, recipe, shift));

        JsonObject data = new JsonObject();
        data.addProperty("window_id", windowId);
        data.addProperty("recipe_id", recipe.id().toString());
        data.addProperty("make_all", shift);
        return ActionResult.ok(data);
    }
}
