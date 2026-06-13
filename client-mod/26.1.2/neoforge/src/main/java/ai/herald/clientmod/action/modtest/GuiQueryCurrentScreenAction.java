package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Returns information about the currently open screen, including widget list
 * if includeWidgets is true (default).
 */
public final class GuiQueryCurrentScreenAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;

        JsonObject data = new JsonObject();
        if (screen == null) {
            data.addProperty("screenClass", "none");
            data.addProperty("screenType", "none");
            data.addProperty("title", "");
            return ActionResult.ok(data);
        }

        data.addProperty("screenClass", screen.getClass().getSimpleName());
        data.addProperty("screenType", classifyScreen(screen));
        data.addProperty("title", screen.getTitle() != null ? screen.getTitle().getString() : "");

        boolean includeWidgets = JsonUtil.getBooleanOrDefault(params, "includeWidgets", true);
        if (includeWidgets) {
            JsonArray widgets = buildWidgetList(screen);
            data.add("widgets", widgets);
        }

        return ActionResult.ok(data);
    }

    private static String classifyScreen(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?>) {
            return "container";
        }
        String name = screen.getClass().getSimpleName().toLowerCase();
        if (name.contains("chat")) return "chat";
        if (name.contains("title")) return "title";
        if (name.contains("inventory")) return "inventory";
        return "other";
    }

    @SuppressWarnings("unchecked")
    private static JsonArray buildWidgetList(Screen screen) {
        JsonArray arr = new JsonArray();
        List<Renderable> renderables = getRenderables(screen);
        if (renderables == null) return arr;

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
        return arr;
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
