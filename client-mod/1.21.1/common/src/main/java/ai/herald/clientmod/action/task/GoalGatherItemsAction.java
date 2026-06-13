package ai.herald.clientmod.action.task;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.TaskManager;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GoalGatherItemsAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        if (!params.has("items") || !params.get("items").isJsonArray()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: items (array)");
        }

        JsonArray items = params.getAsJsonArray("items");
        JsonArray needed = new JsonArray();
        JsonArray have = new JsonArray();

        for (int i = 0; i < items.size(); i++) {
            JsonObject itemReq = items.get(i).getAsJsonObject();
            String itemId = JsonUtil.getStringOrDefault(itemReq, "itemId", "");
            int count = itemReq.has("count") ? itemReq.get("count").getAsInt() : 1;

            int inInventory = countItemInInventory(player, itemId);

            JsonObject entry = new JsonObject();
            entry.addProperty("itemId", itemId);
            entry.addProperty("required", count);
            entry.addProperty("inInventory", inInventory);

            if (inInventory >= count) {
                have.add(entry);
            } else {
                entry.addProperty("stillNeeded", count - inInventory);
                needed.add(entry);
            }
        }

        // Create a task for tracking
        JsonArray steps = new JsonArray();
        steps.add("gather_items");
        String taskId = TaskManager.create("gather_items", steps, "abort");

        JsonObject data = new JsonObject();
        data.addProperty("taskId", taskId);
        data.add("needed", needed);
        data.add("have", have);
        data.addProperty("allSatisfied", needed.size() == 0);
        return ActionResult.ok(data);
    }

    private int countItemInInventory(LocalPlayer player, String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        var item = BuiltInRegistries.ITEM.get(rl);
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && McVersionCompat.isSameItem(stack.getItem(), item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
