package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Types text into an EditBox (text field) on the current screen.
 */
public final class GuiTypeTextAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Screen screen = mc.screen;
        if (screen == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "No screen is currently open");
        }

        String text = JsonUtil.requireString(params, "text");
        int fieldIndex = JsonUtil.getIntOrDefault(params, "fieldIndex", 0);

        List<EditBox> editBoxes = findEditBoxes(screen);
        if (editBoxes.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No text fields found on screen");
        }
        if (fieldIndex < 0 || fieldIndex >= editBoxes.size()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "fieldIndex " + fieldIndex + " out of range (0-" + (editBoxes.size() - 1) + ")");
        }

        EditBox box = editBoxes.get(fieldIndex);
        box.setValue(text);

        JsonObject data = new JsonObject();
        data.addProperty("typed", true);
        data.addProperty("fieldIndex", fieldIndex);
        data.addProperty("value", box.getValue());
        return ActionResult.ok(data);
    }

    @SuppressWarnings("unchecked")
    private static List<EditBox> findEditBoxes(Screen screen) {
        List<EditBox> result = new ArrayList<>();
        try {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof EditBox eb) {
                                result.add(eb);
                            }
                        }
                        if (!result.isEmpty()) return result;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }
}