package ai.herald.clientmod.action.task;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class GoalBuildStructureAction implements ActionExecutor {
    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in game");
        }

        if (!params.has("blueprint") || !params.get("blueprint").isJsonArray()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required param: blueprint (array)");
        }
        if (!params.has("originX") || !params.has("originY") || !params.has("originZ")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required params: originX, originY, originZ");
        }

        JsonArray blueprint = params.getAsJsonArray("blueprint");
        int originX = params.get("originX").getAsInt();
        int originY = params.get("originY").getAsInt();
        int originZ = params.get("originZ").getAsInt();

        // Count blocks needed by type
        Map<String, Integer> blockCounts = new HashMap<>();
        int totalBlocks = 0;

        for (int i = 0; i < blueprint.size(); i++) {
            JsonObject entry = blueprint.get(i).getAsJsonObject();
            String block = JsonUtil.getStringOrDefault(entry, "block", "");
            if (block.isEmpty()) continue;
            blockCounts.merge(block, 1, Integer::sum);
            totalBlocks++;
        }

        // Check inventory for materials
        JsonArray missingMaterials = new JsonArray();
        boolean hasMaterials = true;

        for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
            String blockId = entry.getKey();
            int needed = entry.getValue();
            int inInventory = countBlockInInventory(player, blockId);

            if (inInventory < needed) {
                hasMaterials = false;
                JsonObject missing = new JsonObject();
                missing.addProperty("block", blockId);
                missing.addProperty("needed", needed);
                missing.addProperty("inInventory", inInventory);
                missing.addProperty("stillNeeded", needed - inInventory);
                missingMaterials.add(missing);
            }
        }

        // Estimate build time: ~1 block per 5 ticks
        int estimatedTicks = totalBlocks * 5;

        JsonObject data = new JsonObject();
        data.addProperty("totalBlocks", totalBlocks);
        data.addProperty("uniqueBlockTypes", blockCounts.size());
        data.addProperty("originX", originX);
        data.addProperty("originY", originY);
        data.addProperty("originZ", originZ);
        data.add("missingMaterials", missingMaterials);
        data.addProperty("hasMaterials", hasMaterials);
        data.addProperty("estimatedTicks", estimatedTicks);
        return ActionResult.ok(data);
    }

    private int countBlockInInventory(LocalPlayer player, String blockId) {
        ResourceLocation rl = ResourceLocation.tryParse(blockId);
        var itemHolder = BuiltInRegistries.ITEM.get(rl).orElse(null);
        if (itemHolder == null) return 0;
        net.minecraft.world.item.Item item = itemHolder.value();
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
