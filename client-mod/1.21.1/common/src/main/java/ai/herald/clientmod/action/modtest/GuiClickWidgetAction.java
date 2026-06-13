package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Clicks a widget by index, by message text, or at raw x,y coordinates.
 */
public final class GuiClickWidgetAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }

        // If raw x,y are provided, simulate a mouse click at those coords
        if (params != null && params.has("x") && params.has("y") && !params.has("index") && !params.has("text")) {
            int x = JsonUtil.requireInt(params, "x");
            int y = JsonUtil.requireInt(params, "y");
            screen.mouseClicked(x, y, 0);
            JsonObject data = new JsonObject();
            data.addProperty("clicked", true);
            data.addProperty("x", x);
            data.addProperty("y", y);
            return ActionResult.ok(data);
        }

        // Find widget by index or text
        List<Renderable> renderables = getRenderables(screen);
        if (renderables == null || renderables.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No widgets found on screen");
        }

        AbstractWidget target = null;
        if (params != null && params.has("index")) {
            int idx = JsonUtil.requireInt(params, "index");
            int current = 0;
            for (Renderable r : renderables) {
                if (r instanceof AbstractWidget w) {
                    if (current == idx) { target = w; break; }
                    current++;
                }
            }
        } else if (params != null && params.has("text")) {
            String text = JsonUtil.requireString(params, "text").toLowerCase();
            for (Renderable r : renderables) {
                if (r instanceof AbstractWidget w) {
                    String msg = w.getMessage() != null ? w.getMessage().getString().toLowerCase() : "";
                    if (msg.contains(text)) { target = w; break; }
                }
            }
        } else {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Provide index, text, or x/y");
        }

        if (target == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Widget not found");
        }

        int cx = target.getX() + target.getWidth() / 2;
        int cy = target.getY() + target.getHeight() / 2;
        target.onClick(cx, cy);

        JsonObject data = new JsonObject();
        data.addProperty("clicked", true);
        data.addProperty("message", target.getMessage() != null ? target.getMessage().getString() : "");
        return ActionResult.ok(data);
    }

    @SuppressWarnings("unchecked")
    private static List<Renderable> getRenderables(Screen screen) {
        try {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Renderable) {
                        return (List<Renderable>) val;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
