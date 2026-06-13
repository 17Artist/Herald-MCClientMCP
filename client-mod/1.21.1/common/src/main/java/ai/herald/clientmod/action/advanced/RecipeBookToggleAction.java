package ai.herald.clientmod.action.advanced;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.world.inventory.RecipeBookType;

/** Port of BlackBoxPro advanced/RecipeBookToggleAction.kt. */
public final class RecipeBookToggleAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String cat = JsonUtil.requireString(params, "category").toLowerCase();
        boolean open = JsonUtil.requireBoolean(params, "open");
        boolean filtering = JsonUtil.requireBoolean(params, "filtering");

        RecipeBookType type;
        switch (cat) {
            case "crafting":      type = RecipeBookType.CRAFTING; break;
            case "furnace":       type = RecipeBookType.FURNACE; break;
            case "blast_furnace": type = RecipeBookType.BLAST_FURNACE; break;
            case "smoker":        type = RecipeBookType.SMOKER; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown category: " + cat);
        }
        ClientPacketListener conn = McHelper.connection();
        if (conn == null) return McHelper.notConnected();
        conn.send(new ServerboundRecipeBookChangeSettingsPacket(type, open, filtering));
        return ActionResult.ok();
    }
}
