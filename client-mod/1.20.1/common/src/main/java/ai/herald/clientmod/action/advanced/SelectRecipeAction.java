package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;

/**
 * Port of BlackBoxPro advanced/SelectRecipeAction.kt — cross-version recipe place packet.
 */
public final class SelectRecipeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int windowId = JsonUtil.requireInt(params, "windowId");
        String recipeId = JsonUtil.requireString(params, "recipeId");
        boolean makeAll = JsonUtil.getBooleanOrDefault(params, "makeAll", false);
        ResourceLocation rl = ResourceLocation.tryParse(recipeId);
        if (rl == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid recipe id: " + recipeId);
        ClientLevel level = McHelper.level();
        ClientPacketListener conn = McHelper.connection();
        if (level == null || conn == null) return McHelper.notInGame();
        Object recipeObj = McVersionCompat.getRecipeByKey(((net.minecraft.world.item.crafting.RecipeManager) McVersionCompat.getRecipeManager(level)), rl);
        if (recipeObj == null) return ActionResult.error(ErrorCode.INVALID_PARAMS, "Recipe not found: " + recipeId);
        try {
            Class<?> pktClass = Class.forName("net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket");
            Constructor<?>[] ctors = pktClass.getConstructors();
            Object packet = null;
            for (Constructor<?> ctor : ctors) {
                if (ctor.getParameterCount() == 3) {
                    packet = ctor.newInstance(windowId, recipeObj, makeAll);
                    break;
                }
            }
            if (packet == null) {
                return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "No compatible ServerboundPlaceRecipePacket constructor");
            }
            conn.send((net.minecraft.network.protocol.Packet<?>) packet);
        } catch (Exception e) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Failed to send place recipe packet: " + e.getMessage());
        }
        return ActionResult.ok();
    }
}
