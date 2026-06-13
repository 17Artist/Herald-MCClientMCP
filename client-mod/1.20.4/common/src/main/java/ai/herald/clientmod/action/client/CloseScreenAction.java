package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

/** Port of BlackBoxPro client/CloseScreenAction.kt. */
public final class CloseScreenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            mc.execute(() -> mc.setScreen(null));
        }
        return ActionResult.ok();
    }
}
