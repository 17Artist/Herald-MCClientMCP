package ai.herald.clientmod.action.input;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

/**
 * Simulates keyboard input.
 * Params:
 *   key (required): GLFW key code (integer) or key name (string like "W", "SPACE", "ESCAPE")
 *   action (optional): "press" (default, press+release), "down" (hold), "up" (release)
 *   text (optional): type a string of characters (ignores key/action params)
 */
public final class KeyboardInputAction implements ActionExecutor {

    private static Method keyPressMethod;
    private static Method charTypedMethod;

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        long window = mc.getWindow().handle();

        // Text typing mode
        if (params.has("text")) {
            String text = params.get("text").getAsString();
            mc.execute(() -> {
                try {
                    for (char c : text.toCharArray()) {
                        invokeCharTyped(mc, window, c, 0);
                    }
                } catch (Exception ignored) {}
            });
            JsonObject data = new JsonObject();
            data.addProperty("typed", text);
            return ActionResult.ok(data);
        }

        // Key press mode
        if (!params.has("key")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: key or text");
        }

        int keyCode = resolveKey(params.get("key"));
        if (keyCode == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid key: " + params.get("key"));
        }

        String action = JsonUtil.getStringOrDefault(params, "action", "press");

        mc.execute(() -> {
            try {
                switch (action) {
                    case "down":
                        invokeKeyPress(mc, window, keyCode, 0, GLFW.GLFW_PRESS, 0);
                        break;
                    case "up":
                        invokeKeyPress(mc, window, keyCode, 0, GLFW.GLFW_RELEASE, 0);
                        break;
                    case "press":
                    default:
                        invokeKeyPress(mc, window, keyCode, 0, GLFW.GLFW_PRESS, 0);
                        invokeKeyPress(mc, window, keyCode, 0, GLFW.GLFW_RELEASE, 0);
                        break;
                }
            } catch (Exception ignored) {}
        });

        JsonObject data = new JsonObject();
        data.addProperty("key", keyCode);
        data.addProperty("action", action);
        return ActionResult.ok(data);
    }

    private static void invokeKeyPress(Minecraft mc, long window, int key, int scancode, int action, int mods) throws Exception {
        if (keyPressMethod == null) {
            keyPressMethod = mc.keyboardHandler.getClass().getDeclaredMethod("keyPress", long.class, int.class, int.class, int.class, int.class);
            keyPressMethod.setAccessible(true);
        }
        keyPressMethod.invoke(mc.keyboardHandler, window, key, scancode, action, mods);
    }

    private static void invokeCharTyped(Minecraft mc, long window, int codepoint, int mods) throws Exception {
        if (charTypedMethod == null) {
            charTypedMethod = mc.keyboardHandler.getClass().getDeclaredMethod("charTyped", long.class, int.class, int.class);
            charTypedMethod.setAccessible(true);
        }
        charTypedMethod.invoke(mc.keyboardHandler, window, codepoint, mods);
    }

    private int resolveKey(com.google.gson.JsonElement keyElem) {
        if (keyElem.isJsonPrimitive() && keyElem.getAsJsonPrimitive().isNumber()) {
            return keyElem.getAsInt();
        }
        String name = keyElem.getAsString().toUpperCase();
        switch (name) {
            case "W": return GLFW.GLFW_KEY_W;
            case "A": return GLFW.GLFW_KEY_A;
            case "S": return GLFW.GLFW_KEY_S;
            case "D": return GLFW.GLFW_KEY_D;
            case "SPACE": return GLFW.GLFW_KEY_SPACE;
            case "SHIFT": case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "CTRL": case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "ALT": case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "ESCAPE": case "ESC": return GLFW.GLFW_KEY_ESCAPE;
            case "ENTER": case "RETURN": return GLFW.GLFW_KEY_ENTER;
            case "TAB": return GLFW.GLFW_KEY_TAB;
            case "BACKSPACE": return GLFW.GLFW_KEY_BACKSPACE;
            case "E": return GLFW.GLFW_KEY_E;
            case "Q": return GLFW.GLFW_KEY_Q;
            case "F": return GLFW.GLFW_KEY_F;
            case "T": return GLFW.GLFW_KEY_T;
            case "R": return GLFW.GLFW_KEY_R;
            case "F1": return GLFW.GLFW_KEY_F1;
            case "F2": return GLFW.GLFW_KEY_F2;
            case "F3": return GLFW.GLFW_KEY_F3;
            case "F5": return GLFW.GLFW_KEY_F5;
            case "F11": return GLFW.GLFW_KEY_F11;
            case "1": return GLFW.GLFW_KEY_1;
            case "2": return GLFW.GLFW_KEY_2;
            case "3": return GLFW.GLFW_KEY_3;
            case "4": return GLFW.GLFW_KEY_4;
            case "5": return GLFW.GLFW_KEY_5;
            case "6": return GLFW.GLFW_KEY_6;
            case "7": return GLFW.GLFW_KEY_7;
            case "8": return GLFW.GLFW_KEY_8;
            case "9": return GLFW.GLFW_KEY_9;
            default:
                // Single character → GLFW key code (A=65, etc.)
                if (name.length() == 1) {
                    char c = name.charAt(0);
                    if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
                    if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
                }
                return -1;
        }
    }
}
