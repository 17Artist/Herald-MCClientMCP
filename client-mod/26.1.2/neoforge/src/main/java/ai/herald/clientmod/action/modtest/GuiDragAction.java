package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Simulates a mouse drag on the current screen: mouseClicked → mouseDragged → mouseReleased.
 */
public final class GuiDragAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }

        int fromX = JsonUtil.requireInt(params, "fromX");
        int fromY = JsonUtil.requireInt(params, "fromY");
        int toX = JsonUtil.requireInt(params, "toX");
        int toY = JsonUtil.requireInt(params, "toY");

        double dx = toX - fromX;
        double dy = toY - fromY;

        McVersionCompat.mouseClicked(screen, fromX, fromY, 0);
        McVersionCompat.mouseDragged(screen, toX, toY, 0, dx, dy);
        McVersionCompat.mouseReleased(screen, toX, toY, 0);

        JsonObject data = new JsonObject();
        data.addProperty("dragged", true);
        data.addProperty("fromX", fromX);
        data.addProperty("fromY", fromY);
        data.addProperty("toX", toX);
        data.addProperty("toY", toY);
        return ActionResult.ok(data);
    }
}
