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

/**
 * Asserts that a container screen is currently open.
 * Optionally checks that the screen's class name contains a given type string.
 */
public final class AssertContainerOpenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        if (mc.player == null) return McHelper.notInGame();

        String type = JsonUtil.getStringOrDefault(params, "type", null);

        Screen screen = mc.screen;
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected a container screen to be open but current screen is "
                    + (screen != null ? screen.getClass().getSimpleName() : "null"));
        }

        if (type != null) {
            String className = screen.getClass().getSimpleName();
            if (!className.toLowerCase().contains(type.toLowerCase())) {
                return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                    "Expected container type containing '" + type + "' but got " + className);
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Container screen is open: " + screen.getClass().getSimpleName());
        return ActionResult.ok(data);
    }
}
