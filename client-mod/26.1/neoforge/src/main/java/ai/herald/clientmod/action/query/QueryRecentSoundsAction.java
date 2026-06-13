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
 * Sync: Return last N sounds from SensingBuffer.
 * Returns: {sounds: [{id, x, y, z, volume, pitch, timestamp}, ...]}
 */
public final class QueryRecentSoundsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int count = JsonUtil.getIntOrDefault(params, "count", 20);

        List<JsonObject> sounds = SensingBuffer.getRecentSounds(count);
        JsonArray arr = new JsonArray();
        for (JsonObject s : sounds) {
            arr.add(s);
        }

        JsonObject data = new JsonObject();
        data.add("sounds", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
