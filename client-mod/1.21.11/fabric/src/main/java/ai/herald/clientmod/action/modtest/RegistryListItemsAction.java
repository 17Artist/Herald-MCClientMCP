package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Lists items from the item registry, filtered by namespace and/or name filter.
 */
public final class RegistryListItemsAction implements ActionExecutor {

    private static final int LIMIT = 500;

    @Override
    public ActionResult execute(JsonObject params) {
        String namespace = JsonUtil.getStringOrDefault(params, "namespace", null);
        String filter = JsonUtil.getStringOrDefault(params, "filter", null);
        String filterLower = filter != null ? filter.toLowerCase() : null;

        JsonArray arr = new JsonArray();
        for (Item item : BuiltInRegistries.ITEM) {
            if (arr.size() >= LIMIT) break;
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (namespace != null && !id.getNamespace().equals(namespace)) continue;
            if (filterLower != null && !id.getPath().toLowerCase().contains(filterLower)) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("namespace", id.getNamespace());
            arr.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("items", arr);
        data.addProperty("count", arr.size());
        data.addProperty("limited", arr.size() >= LIMIT);
        return ActionResult.ok(data);
    }
}
