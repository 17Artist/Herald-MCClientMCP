package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.ItemStackSerializer;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Port of BlackBoxPro QueryContainerSlotsAction.kt to Java + Mojang 1.20.1. */
public final class QueryContainerSlotsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        AbstractContainerMenu handler = player.containerMenu;
        int windowId = JsonUtil.getIntOrDefault(params, "windowId", handler.containerId);
        int size = handler.slots.size();

        List<Integer> slotIndices = new ArrayList<>();
        if (params != null && params.has("slots") && params.get("slots").isJsonArray()) {
            JsonArray req = params.getAsJsonArray("slots");
            for (JsonElement el : req) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                    slotIndices.add(el.getAsInt());
                }
            }
        } else {
            for (int i = 0; i < size; i++) slotIndices.add(i);
        }

        JsonObject slotsObj = new JsonObject();
        for (int idx : slotIndices) {
            if (idx < 0 || idx >= size) continue;
            Slot slot = handler.slots.get(idx);
            ItemStack stack = slot.getItem();
            slotsObj.add(Integer.toString(idx), ItemStackSerializer.serializeSlot(stack));
        }

        JsonObject data = new JsonObject();
        data.addProperty("windowId", windowId);
        data.add("slots", slotsObj);
        return ActionResult.ok(data);
    }
}
