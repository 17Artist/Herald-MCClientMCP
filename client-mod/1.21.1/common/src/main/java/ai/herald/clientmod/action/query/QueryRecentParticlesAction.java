package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.sensing.SensingBuffer;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * Sync: Return last N particles from SensingBuffer.
 * Returns: {particles: [{type, x, y, z, timestamp}, ...]}
 */
public final class QueryRecentParticlesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int count = JsonUtil.getIntOrDefault(params, "count", 20);

        List<JsonObject> particles = SensingBuffer.getRecentParticles(count);
        JsonArray arr = new JsonArray();
        for (JsonObject p : particles) {
            arr.add(p);
        }

        JsonObject data = new JsonObject();
        data.add("particles", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
