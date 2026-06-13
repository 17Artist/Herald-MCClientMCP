package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Takes a screenshot of the current screen using vanilla screenshot utility.
 */
public final class GuiScreenshotElementAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();

        mc.execute(() -> Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> {}));

        JsonObject data = new JsonObject();
        data.addProperty("captured", true);
        data.addProperty("screenOpen", mc.screen != null);
        if (mc.screen != null) {
            data.addProperty("screenClass", mc.screen.getClass().getSimpleName());
        }
        return ActionResult.ok(data);
    }
}
