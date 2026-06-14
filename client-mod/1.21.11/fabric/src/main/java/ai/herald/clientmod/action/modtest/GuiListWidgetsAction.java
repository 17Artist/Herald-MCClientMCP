package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Lists all Renderable widgets on the current screen with position and type info.
 */
public final class GuiListWidgetsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }

        List<Renderable> renderables = getRenderables(screen);
        JsonArray arr = new JsonArray();

        if (renderables != null) {
            int index = 0;
            for (Renderable r : renderables) {
                if (r instanceof AbstractWidget widget) {
                    JsonObject w = new JsonObject();
                    w.addProperty("index", index);
                    w.addProperty("type", classifyWidget(widget));
                    w.addProperty("x", widget.getX());
                    w.addProperty("y", widget.getY());
                    w.addProperty("width", widget.getWidth());
                    w.addProperty("height", widget.getHeight());
                    w.addProperty("message", widget.getMessage() != null ? widget.getMessage().getString() : "");
                    w.addProperty("active", widget.active);
                    arr.add(w);
                }
                index++;
            }
        }

        JsonObject data = new JsonObject();
        data.add("widgets", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }

    private static String classifyWidget(AbstractWidget widget) {
        if (widget instanceof EditBox) return "textField";
        if (widget instanceof Button) return "button";
        String name = widget.getClass().getSimpleName().toLowerCase();
        if (name.contains("slider")) return "slider";
        if (name.contains("checkbox")) return "checkbox";
        return "button";
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
