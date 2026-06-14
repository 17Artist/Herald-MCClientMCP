package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

/**
 * Gracefully shuts down the MC client.
 * Calls Minecraft.stop() which triggers proper cleanup and JVM exit.
 */
public final class ShutdownClientAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        JsonObject data = new JsonObject();
        data.addProperty("shutting_down", true);

        // Schedule shutdown on next tick to allow HTTP response to be sent first
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.stop());

        return ActionResult.ok(data);
    }
}
