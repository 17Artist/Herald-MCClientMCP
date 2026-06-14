package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Port of BlackBoxPro QuerySlotTooltipAction.kt to Java + Mojang 1.20.1. */
public final class QuerySlotTooltipAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int slotIndex = JsonUtil.requireInt(params, "slot");
        boolean advanced = JsonUtil.getBooleanOrDefault(params, "advanced", false);

        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        if (player == null) return McHelper.notInGame();

        AbstractContainerMenu handler;
        Screen screen = mc.screen;
        if (screen instanceof AbstractContainerScreen<?> acs) {
            handler = acs.getMenu();
        } else {
            handler = player.containerMenu;
        }

        int size = handler.slots.size();
        if (slotIndex < 0 || slotIndex >= size) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Slot index " + slotIndex + " out of range [0, " + size + ")");
        }

        ItemStack stack = handler.slots.get(slotIndex).getItem();
        if (stack.isEmpty()) {
            JsonObject data = new JsonObject();
            data.addProperty("slot", slotIndex);
            data.addProperty("empty", true);
            return ActionResult.ok(data);
        }

        TooltipFlag flag = advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> tooltipLines = McVersionCompat.getTooltipLines(stack, player.level(), player, flag);

        JsonObject data = new JsonObject();
        data.addProperty("slot", slotIndex);
        data.addProperty("empty", false);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        data.addProperty("itemId", itemId != null ? itemId.toString() : "unknown");
        data.addProperty("itemName", stack.getHoverName().getString());
        data.addProperty("count", stack.getCount());
        data.addProperty("damage", stack.getDamageValue());
        data.addProperty("maxDamage", stack.getMaxDamage());

        JsonArray plain = new JsonArray();
        JsonArray formatted = new JsonArray();
        JsonArray jsonForm = new JsonArray();
        for (Component line : tooltipLines) {
            plain.add(line.getString());
            // 1.20.1 has no public legacy-string helper — use plain string.
            formatted.add(line.getString());
            try {
                jsonForm.add(JsonParser.parseString(McVersionCompat.componentToJson(line)));
            } catch (Throwable t) {
                jsonForm.add(line.getString());
            }
        }
        data.add("tooltip", plain);
        data.add("tooltipFormatted", formatted);
        data.add("tooltipJson", jsonForm);

        return ActionResult.ok(data);
    }
}
