package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/**
 * equip_item — moves an item from a source slot to an equipment slot.
 * Params: slot(int, source slot), equipSlot(head/chest/legs/feet/offhand)
 *
 * Equipment slots in player inventory container:
 *   head=5, chest=6, legs=7, feet=8, offhand=45
 * Player inventory raw slots:
 *   head=39, chest=38, legs=37, feet=36, offhand=40
 * Container slot mapping for player inventory screen:
 *   crafting: 0-4, armor: 5-8, inventory: 9-35, hotbar: 36-44, offhand: 45
 */
public final class EquipItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        // Accept both "slot" and "sourceSlot" for the source inventory slot
        int sourceSlot;
        if (params.has("slot")) {
            sourceSlot = JsonUtil.requireInt(params, "slot");
        } else if (params.has("sourceSlot")) {
            sourceSlot = JsonUtil.requireInt(params, "sourceSlot");
        } else {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Missing required param: slot (int, source inventory slot index 0-40)");
        }
        String equipSlot;
        if (params.has("equipSlot")) {
            equipSlot = JsonUtil.requireString(params, "equipSlot");
        } else if (params.has("targetSlot")) {
            equipSlot = JsonUtil.requireString(params, "targetSlot");
        } else {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Missing required param: equipSlot (string: head, chest, legs, feet, offhand)");
        }

        int targetContainerSlot = resolveEquipSlot(equipSlot);
        if (targetContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Invalid equipSlot: " + equipSlot + ". Use: head, chest, legs, feet, offhand");
        }

        // Convert raw inventory slot to container slot for the player inventory screen
        // Raw slots 0-8 = hotbar → container 36-44
        // Raw slots 9-35 = main inventory → container 9-35
        // Raw slots 36-39 = armor → container 8,7,6,5
        // Raw slot 40 = offhand → container 45
        int sourceContainerSlot = rawToContainerSlot(sourceSlot);
        if (sourceContainerSlot == -1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Invalid source slot: " + sourceSlot);
        }

        AbstractContainerMenu menu = player.inventoryMenu;
        int containerId = menu.containerId;
        int stateId = menu.getStateId();

        // Pick up from source
        Int2ObjectMap<ItemStack> changed1 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId, sourceContainerSlot, 0, ClickType.PICKUP,
                menu.getCarried(), changed1);

        // Place into target equipment slot
        Int2ObjectMap<ItemStack> changed2 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 1, targetContainerSlot, 0, ClickType.PICKUP,
                menu.getCarried(), changed2);

        // If there was an item in the equip slot, put it back in source
        Int2ObjectMap<ItemStack> changed3 = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
                containerId, stateId + 2, sourceContainerSlot, 0, ClickType.PICKUP,
                menu.getCarried(), changed3);

        JsonObject data = new JsonObject();
        data.addProperty("fromSlot", sourceSlot);
        data.addProperty("toEquipSlot", equipSlot);
        data.addProperty("success", true);
        return ActionResult.ok(data);
    }

    private static int resolveEquipSlot(String name) {
        switch (name.toLowerCase()) {
            case "head": return 5;
            case "chest": return 6;
            case "legs": return 7;
            case "feet": return 8;
            case "offhand": return 45;
            default: return -1;
        }
    }

    private static int rawToContainerSlot(int raw) {
        if (raw >= 0 && raw <= 8) return raw + 36;   // hotbar
        if (raw >= 9 && raw <= 35) return raw;        // main inventory
        if (raw == 36) return 8;                      // feet
        if (raw == 37) return 7;                      // legs
        if (raw == 38) return 6;                      // chest
        if (raw == 39) return 5;                      // head
        if (raw == 40) return 45;                     // offhand
        return -1;
    }
}
