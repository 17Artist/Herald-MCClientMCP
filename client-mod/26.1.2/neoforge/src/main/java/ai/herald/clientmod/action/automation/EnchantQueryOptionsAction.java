package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;


/**
 * Sync: Query the 3 enchantment options from an open enchanting table.
 * No params required. Returns the cost, enchantment clue, and level clue for each slot.
 */
public final class EnchantQueryOptionsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "No enchanting table GUI is open");
        }

        JsonArray options = new JsonArray();
        for (int i = 0; i < 3; i++) {
            JsonObject option = new JsonObject();
            option.addProperty("slot", i);
            option.addProperty("cost", menu.costs[i]);

            // enchantClue contains the enchantment id (int registry index)
            int clueId = menu.enchantClue[i];
            if (clueId >= 0) {
                Object ench = McVersionCompat.enchantmentById(clueId);
                if (ench != null) {
                    Identifier key = McVersionCompat.enchantmentRegistryKey(ench);
                    option.addProperty("enchantment", key != null ? key.toString() : "unknown");
                }
            }

            int levelClue = menu.levelClue[i];
            if (levelClue > 0) {
                option.addProperty("level", levelClue);
            }

            options.add(option);
        }

        JsonObject data = new JsonObject();
        data.add("options", options);
        return ActionResult.ok(data);
    }
}
