package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.scan.AreaScanner;
import ai.herald.clientmod.testing.SnapshotManager;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * snapshot_create — captures current player state into a named snapshot.
 * Params: name(string), includeBlocks?(boolean, false), radius?(int, 8)
 */
public final class SnapshotCreateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String name = JsonUtil.requireString(params, "name");
        boolean includeBlocks = JsonUtil.getBooleanOrDefault(params, "includeBlocks", false);
        int radius = JsonUtil.getIntOrDefault(params, "radius", 8);

        JsonObject snapshot = new JsonObject();
        long timestamp = System.currentTimeMillis();
        snapshot.addProperty("timestamp", timestamp);

        // Position
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        position.addProperty("yaw", player.getYRot());
        position.addProperty("pitch", player.getXRot());
        snapshot.add("position", position);

        // Health & food
        snapshot.addProperty("health", player.getHealth());
        snapshot.addProperty("food", player.getFoodData().getFoodLevel());
        snapshot.addProperty("saturation", player.getFoodData().getSaturationLevel());

        // Gamemode
        Minecraft mc = McHelper.mc();
        MultiPlayerGameMode gm = mc.gameMode;
        snapshot.addProperty("gamemode", gm != null && gm.getPlayerMode() != null
                ? gm.getPlayerMode().name() : "unknown");

        // Dimension
        ClientLevel level = McHelper.level();
        snapshot.addProperty("dimension", level != null
                ? level.dimension().identifier().toString() : "unknown");

        // Inventory (all 41 slots)
        JsonArray inventoryArr = new JsonArray();
        Inventory inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            ItemStack stack = inventory.getItem(i);
            JsonObject slotObj = new JsonObject();
            slotObj.addProperty("slot", i);
            if (stack.isEmpty()) {
                slotObj.addProperty("itemId", "minecraft:air");
                slotObj.addProperty("count", 0);
            } else {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                slotObj.addProperty("itemId", itemId != null ? itemId.toString() : "minecraft:air");
                slotObj.addProperty("count", stack.getCount());
            }
            inventoryArr.add(slotObj);
        }
        snapshot.add("inventory", inventoryArr);

        // Active effects
        JsonArray effectsArr = new JsonArray();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            Identifier id = McVersionCompat.mobEffectIdFromInstance(instance);
            JsonObject eff = new JsonObject();
            eff.addProperty("id", id != null ? id.toString() : "unknown");
            eff.addProperty("amplifier", instance.getAmplifier());
            eff.addProperty("duration", instance.getDuration());
            effectsArr.add(eff);
        }
        snapshot.add("effects", effectsArr);

        // Optional: blocks in radius
        if (includeBlocks && level != null) {
            BlockPos playerPos = player.blockPosition();
            BlockPos from = playerPos.offset(-radius, -radius, -radius);
            BlockPos to = playerPos.offset(radius, radius, radius);
            long volume = (long)(radius * 2 + 1) * (radius * 2 + 1) * (radius * 2 + 1);
            if (volume <= AreaScanner.MAX_VOLUME) {
                JsonObject blocks = AreaScanner.scanPalette(level, from, to);
                snapshot.add("blocks", blocks);
            }
        }

        // Store snapshot
        SnapshotManager.save(name, snapshot);

        // Build response
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("timestamp", timestamp);
        JsonArray keys = new JsonArray();
        for (String key : snapshot.keySet()) {
            keys.add(key);
        }
        data.add("keys", keys);
        return ActionResult.ok(data);
    }
}
