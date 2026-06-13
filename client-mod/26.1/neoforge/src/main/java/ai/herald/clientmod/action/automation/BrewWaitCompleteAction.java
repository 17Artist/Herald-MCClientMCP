package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;

/**
 * Sync: Check if brewing is complete.
 * Params: timeout? (int, ticks — not used in sync mode, reserved for future async).
 * Returns whether brewing is done (progress == 0 and was previously > 0).
 */
public final class BrewWaitCompleteAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof BrewingStandMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No brewing stand GUI is open");
        }

        int brewTime = McVersionCompat.getMenuData(menu, "brewingStandData", 0);

        JsonObject data = new JsonObject();
        if (brewTime == 0) {
            data.addProperty("complete", true);
            data.addProperty("progress", 0);
        } else {
            data.addProperty("complete", false);
            data.addProperty("progress", brewTime);
            data.addProperty("message", "Still brewing, " + brewTime + " ticks remaining");
        }
        return ActionResult.ok(data);
    }
}
