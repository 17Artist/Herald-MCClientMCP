package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;

/** Port of BlackBoxPro QueryWorldStateAction.kt to Java + Mojang 1.20.1. */
public final class QueryWorldStateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        JsonObject data = new JsonObject();
        data.addProperty("timeOfDay", level.getDayTime());
        data.addProperty("worldTime", level.getGameTime());
        data.addProperty("raining", level.isRaining());
        data.addProperty("thundering", level.isThundering());
        data.addProperty("dimension", level.dimension().location().toString());
        data.addProperty("hasSkyLight", level.dimensionType().hasSkyLight());
        data.addProperty("hasCeiling", level.dimensionType().hasCeiling());
        data.addProperty("difficulty", level.getDifficulty().getKey());
        data.addProperty("seaLevel", level.getSeaLevel());
        return ActionResult.ok(data);
    }
}
