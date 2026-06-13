package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Lists all known dimension IDs from the connection.
 */
public final class RegistryListDimensionsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientPacketListener connection = McHelper.connection();
        if (connection == null) return McHelper.notConnected();

        Set<ResourceKey<Level>> levels = connection.levels();
        JsonArray arr = new JsonArray();

        for (ResourceKey<Level> key : levels) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", key.identifier().toString());
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("dimensions", arr);
        data.addProperty("count", arr.size());
        return ActionResult.ok(data);
    }
}
