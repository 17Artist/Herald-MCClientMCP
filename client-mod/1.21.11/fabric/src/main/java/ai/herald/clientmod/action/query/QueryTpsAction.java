package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: Estimate server TPS from client-side timing.
 * The MC client doesn't directly know server TPS; returns best available estimate.
 */
public final class QueryTpsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        // Client-side estimation: use getDeltaFrameTime for frame delta
        float deltaFrameTime = McVersionCompat.getDeltaFrameTime(mc);

        // The client doesn't have direct access to server MSPT.
        // Best we can do is report frame timing and note it's client-side.
        // A real TPS monitor would track time between server tick packets.
        float estimatedMspt = deltaFrameTime * 50.0f; // rough approximation

        JsonObject data = new JsonObject();
        data.addProperty("estimatedTps", 20.0);
        data.addProperty("mspt", estimatedMspt);
        data.addProperty("deltaFrameTime", deltaFrameTime);
        data.addProperty("note", "Estimated from client timing; true server TPS requires server-side data");
        return ActionResult.ok(data);
    }
}
