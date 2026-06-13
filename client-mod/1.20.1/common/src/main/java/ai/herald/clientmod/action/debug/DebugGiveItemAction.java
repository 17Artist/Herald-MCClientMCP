package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DebugGiveItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String itemId = JsonUtil.requireString(params, "itemId");
        int count = JsonUtil.getIntOrDefault(params, "count", 1);

        Minecraft mc = McHelper.mc();

        if (mc.getSingleplayerServer() != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (sp == null) return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Cannot get server player");

            String fullId = itemId.contains(":") ? itemId : "minecraft:" + itemId;
            Item item = McVersionCompat.registryGet(BuiltInRegistries.ITEM, ResourceLocation.tryParse(fullId));
            if (item == Items.AIR && !fullId.equals("minecraft:air")) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "Unknown item: " + itemId);
            }

            ItemStack stack = new ItemStack(item, count);
            boolean added = sp.getInventory().add(stack);
            if (!added) {
                sp.drop(stack, false);
            }

            JsonObject data = new JsonObject();
            data.addProperty("itemId", fullId);
            data.addProperty("count", count);
            data.addProperty("given", true);
            return ActionResult.ok(data);
        } else {
            String nbt = JsonUtil.getStringOrDefault(params, "nbt", "");
            String cmd = "give @s " + itemId + nbt + " " + count;
            player.connection.sendCommand(cmd);
            JsonObject data = new JsonObject();
            data.addProperty("command", cmd);
            return ActionResult.ok(data);
        }
    }
}
