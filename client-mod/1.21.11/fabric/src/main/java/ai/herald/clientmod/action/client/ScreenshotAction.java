package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Port of BlackBoxPro client/ScreenshotAction.kt — saves a screenshot using vanilla helper. */
public final class ScreenshotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> { }));
        return ActionResult.ok();
    }
}
