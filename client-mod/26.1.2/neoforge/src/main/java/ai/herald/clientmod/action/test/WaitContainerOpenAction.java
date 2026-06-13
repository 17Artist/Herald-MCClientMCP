package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Sync check: test if a container screen is currently open.
 * Returns ok with screen info if open, error ASSERTION_FAILED if not.
 */
public final class WaitContainerOpenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String expectedType = JsonUtil.getStringOrDefault(params, "type", null);

        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;

        if (screen == null) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED, "No screen open");
        }

        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                    "Screen open but not a container: " + screen.getClass().getSimpleName());
        }

        String screenClass = screen.getClass().getSimpleName();
        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";

        if (expectedType != null && !screenClass.toLowerCase().contains(expectedType.toLowerCase())
                && !title.toLowerCase().contains(expectedType.toLowerCase())) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                    "Container open but type mismatch: expected=" + expectedType
                            + " actual=" + screenClass + " title=" + title);
        }

        JsonObject data = new JsonObject();
        data.addProperty("open", true);
        data.addProperty("screenClass", screenClass);
        data.addProperty("title", title);

        LocalPlayer player = mc.player;
        if (player != null) {
            AbstractContainerMenu menu = player.containerMenu;
            data.addProperty("windowId", menu.containerId);
            data.addProperty("slotCount", menu.slots.size());
        }

        return ActionResult.ok(data);
    }
}
