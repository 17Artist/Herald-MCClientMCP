package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.testing.SnapshotManager;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * snapshot_compare — compares a stored snapshot with current state or another snapshot.
 * Params: name(string), compareWith?("current" or snapshot name, default "current")
 */
public final class SnapshotCompareAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String name = JsonUtil.requireString(params, "name");
        String compareWith = JsonUtil.getStringOrDefault(params, "compareWith", "current");

        JsonObject snapshot = SnapshotManager.get(name);
        if (snapshot == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Snapshot not found: " + name);
        }

        JsonObject other;
        if ("current".equals(compareWith)) {
            other = buildCurrentState(player);
        } else {
            other = SnapshotManager.get(compareWith);
            if (other == null) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                        "Snapshot not found: " + compareWith);
            }
        }

        JsonArray changed = new JsonArray();
        JsonArray unchanged = new JsonArray();

        // Compare position (threshold: 1 block)
        comparePosition(snapshot, other, changed, unchanged);

        // Compare health
        compareNumber(snapshot, other, "health", changed, unchanged);

        // Compare food
        compareNumber(snapshot, other, "food", changed, unchanged);

        // Compare saturation
        compareNumber(snapshot, other, "saturation", changed, unchanged);

        // Compare dimension
        compareString(snapshot, other, "dimension", changed, unchanged);

        // Compare gamemode
        compareString(snapshot, other, "gamemode", changed, unchanged);

        // Compare inventory
        compareInventory(snapshot, other, changed, unchanged);

        JsonObject data = new JsonObject();
        data.add("changed", changed);
        data.add("unchanged", unchanged);
        return ActionResult.ok(data);
    }

    private JsonObject buildCurrentState(LocalPlayer player) {
        JsonObject state = new JsonObject();

        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        state.add("position", position);

        state.addProperty("health", player.getHealth());
        state.addProperty("food", player.getFoodData().getFoodLevel());
        state.addProperty("saturation", player.getFoodData().getSaturationLevel());

        Minecraft mc = McHelper.mc();
        MultiPlayerGameMode gm = mc.gameMode;
        state.addProperty("gamemode", gm != null && gm.getPlayerMode() != null
                ? gm.getPlayerMode().name() : "unknown");

        ClientLevel level = McHelper.level();
        state.addProperty("dimension", level != null
                ? level.dimension().location().toString() : "unknown");

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
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                slotObj.addProperty("itemId", itemId != null ? itemId.toString() : "minecraft:air");
                slotObj.addProperty("count", stack.getCount());
            }
            inventoryArr.add(slotObj);
        }
        state.add("inventory", inventoryArr);
        return state;
    }

    private void comparePosition(JsonObject snap, JsonObject other,
                                 JsonArray changed, JsonArray unchanged) {
        JsonObject snapPos = snap.has("position") ? snap.getAsJsonObject("position") : null;
        JsonObject otherPos = other.has("position") ? other.getAsJsonObject("position") : null;
        if (snapPos == null || otherPos == null) return;

        double dx = snapPos.get("x").getAsDouble() - otherPos.get("x").getAsDouble();
        double dy = snapPos.get("y").getAsDouble() - otherPos.get("y").getAsDouble();
        double dz = snapPos.get("z").getAsDouble() - otherPos.get("z").getAsDouble();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 1.0) {
            JsonObject diff = new JsonObject();
            diff.addProperty("field", "position");
            diff.add("snapshotValue", snapPos);
            diff.add("currentValue", otherPos);
            changed.add(diff);
        } else {
            unchanged.add("position");
        }
    }

    private void compareNumber(JsonObject snap, JsonObject other, String field,
                               JsonArray changed, JsonArray unchanged) {
        if (!snap.has(field) || !other.has(field)) return;
        double snapVal = snap.get(field).getAsDouble();
        double otherVal = other.get(field).getAsDouble();
        if (Math.abs(snapVal - otherVal) > 0.01) {
            JsonObject diff = new JsonObject();
            diff.addProperty("field", field);
            diff.addProperty("snapshotValue", snapVal);
            diff.addProperty("currentValue", otherVal);
            changed.add(diff);
        } else {
            unchanged.add(field);
        }
    }

    private void compareString(JsonObject snap, JsonObject other, String field,
                               JsonArray changed, JsonArray unchanged) {
        if (!snap.has(field) || !other.has(field)) return;
        String snapVal = snap.get(field).getAsString();
        String otherVal = other.get(field).getAsString();
        if (!snapVal.equals(otherVal)) {
            JsonObject diff = new JsonObject();
            diff.addProperty("field", field);
            diff.addProperty("snapshotValue", snapVal);
            diff.addProperty("currentValue", otherVal);
            changed.add(diff);
        } else {
            unchanged.add(field);
        }
    }

    private void compareInventory(JsonObject snap, JsonObject other,
                                  JsonArray changed, JsonArray unchanged) {
        if (!snap.has("inventory") || !other.has("inventory")) return;
        JsonArray snapInv = snap.getAsJsonArray("inventory");
        JsonArray otherInv = other.getAsJsonArray("inventory");

        boolean anyChange = false;
        JsonArray invChanges = new JsonArray();

        int size = Math.min(snapInv.size(), otherInv.size());
        for (int i = 0; i < size; i++) {
            JsonObject s = snapInv.get(i).getAsJsonObject();
            JsonObject o = otherInv.get(i).getAsJsonObject();
            String sItem = s.get("itemId").getAsString();
            String oItem = o.get("itemId").getAsString();
            int sCount = s.get("count").getAsInt();
            int oCount = o.get("count").getAsInt();

            if (!sItem.equals(oItem) || sCount != oCount) {
                anyChange = true;
                JsonObject slotDiff = new JsonObject();
                slotDiff.addProperty("slot", i);
                slotDiff.addProperty("wasItem", sItem);
                slotDiff.addProperty("wasCount", sCount);
                slotDiff.addProperty("nowItem", oItem);
                slotDiff.addProperty("nowCount", oCount);
                invChanges.add(slotDiff);
            }
        }

        if (anyChange) {
            JsonObject diff = new JsonObject();
            diff.addProperty("field", "inventory");
            diff.add("changes", invChanges);
            changed.add(diff);
        } else {
            unchanged.add("inventory");
        }
    }
}
