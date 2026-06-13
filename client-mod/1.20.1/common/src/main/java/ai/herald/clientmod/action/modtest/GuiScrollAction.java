package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Simulates a mouse scroll event on the current screen.
 */
public final class GuiScrollAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }

        String direction = JsonUtil.getStringOrDefault(params, "direction", "down");
        int amount = JsonUtil.getIntOrDefault(params, "amount", 3);

        double centerX = mc.getWindow().getGuiScaledWidth() / 2.0;
        double centerY = mc.getWindow().getGuiScaledHeight() / 2.0;
        double scrollDelta = "up".equalsIgnoreCase(direction) ? amount : -amount;

        screen.mouseScrolled(centerX, centerY, scrollDelta);

        JsonObject data = new JsonObject();
        data.addProperty("scrolled", true);
        data.addProperty("direction", direction);
        data.addProperty("amount", amount);
        return ActionResult.ok(data);
    }
}
