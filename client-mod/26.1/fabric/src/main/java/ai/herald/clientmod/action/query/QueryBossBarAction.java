package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

/**
 * Port of BlackBoxPro QueryBossBarAction.kt to Java + Mojang 1.20.1.
 *
 * <p>1.20.1 caveat: {@link BossHealthOverlay#events} is private with no public
 * accessor — we reflect into it. On any failure we return an empty list with
 * {@code available=false}.</p>
 */
public final class QueryBossBarAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        BossHealthOverlay overlay = mc.gui.getBossOverlay();

        JsonArray bars = new JsonArray();
        boolean available = false;
        try {
            Field f = BossHealthOverlay.class.getDeclaredField("events");
            f.setAccessible(true);
            Object raw = f.get(overlay);
            if (raw instanceof Map<?, ?> map) {
                available = true;
                for (Object value : map.values()) {
                    if (value instanceof BossEvent bar) {
                        JsonObject b = new JsonObject();
                        b.addProperty("name", bar.getName().getString());
                        b.addProperty("percent", bar.getProgress());
                        b.addProperty("color", bar.getColor().name().toLowerCase(Locale.ROOT));
                        b.addProperty("style", bar.getOverlay().name().toLowerCase(Locale.ROOT));
                        b.addProperty("darkenSky", bar.shouldDarkenScreen());
                        b.addProperty("playMusic", bar.shouldPlayBossMusic());
                        b.addProperty("createFog", bar.shouldCreateWorldFog());
                        bars.add(b);
                    }
                }
            }
        } catch (Throwable ignored) {
            // reflection failed — leave bars empty, available=false
        }

        JsonObject data = new JsonObject();
        data.add("bossBars", bars);
        data.addProperty("count", bars.size());
        data.addProperty("available", available);
        return ActionResult.ok(data);
    }
}
