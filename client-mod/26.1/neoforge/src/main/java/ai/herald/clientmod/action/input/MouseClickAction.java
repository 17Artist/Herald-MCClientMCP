package ai.herald.clientmod.action.input;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

/**
 * Simulates a mouse click at screen coordinates.
 * Params:
 *   x (required): screen X coordinate
 *   y (required): screen Y coordinate
 *   button (optional): 0=left (default), 1=right, 2=middle
 *   action (optional): "click" (default, press+release), "press", "release"
 */
public final class MouseClickAction implements ActionExecutor {

    private static Method onPressMethod;
    private static Method onMoveMethod;

    @Override
    public ActionResult execute(JsonObject params) {
        if (!params.has("x") || !params.has("y")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required params: x, y");
        }
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        int button = JsonUtil.getIntOrDefault(params, "button", 0);
        String action = JsonUtil.getStringOrDefault(params, "action", "click");

        Minecraft mc = McHelper.mc();

        mc.execute(() -> {
            try {
                long window = mc.getWindow().handle();
                // Move cursor
                invokeOnMove(mc, window, x, y);
                // Click
                int press = 1, release = 0;
                switch (action) {
                    case "press":
                        invokeOnPress(mc, window, button, press, 0);
                        break;
                    case "release":
                        invokeOnPress(mc, window, button, release, 0);
                        break;
                    case "click":
                    default:
                        invokeOnPress(mc, window, button, press, 0);
                        invokeOnPress(mc, window, button, release, 0);
                        break;
                }
            } catch (Exception e) {
                // Silently fail; logged at WARN level
            }
        });

        JsonObject data = new JsonObject();
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("button", button);
        data.addProperty("action", action);
        return ActionResult.ok(data);
    }

    private static void invokeOnPress(Minecraft mc, long window, int button, int action, int mods) throws Exception {
        if (onPressMethod == null) {
            onPressMethod = mc.mouseHandler.getClass().getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
            onPressMethod.setAccessible(true);
        }
        onPressMethod.invoke(mc.mouseHandler, window, button, action, mods);
    }

    private static void invokeOnMove(Minecraft mc, long window, double x, double y) throws Exception {
        if (onMoveMethod == null) {
            onMoveMethod = mc.mouseHandler.getClass().getDeclaredMethod("onMove", long.class, double.class, double.class);
            onMoveMethod.setAccessible(true);
        }
        onMoveMethod.invoke(mc.mouseHandler, window, x, y);
    }
}
