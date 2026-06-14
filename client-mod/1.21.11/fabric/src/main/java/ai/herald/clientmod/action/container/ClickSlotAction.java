package ai.herald.clientmod.action.container;

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

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** Port of BlackBoxPro container/ClickSlotAction.kt. */
public final class ClickSlotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int windowId = JsonUtil.requireInt(params, "windowId");
        int stateId = JsonUtil.requireInt(params, "stateId");
        int slot = JsonUtil.requireInt(params, "slot");
        int button = JsonUtil.requireInt(params, "button");
        int mode = JsonUtil.requireInt(params, "mode");

        ClickType type;
        switch (mode) {
            case 0: type = ClickType.PICKUP; break;
            case 1: type = ClickType.QUICK_MOVE; break;
            case 2: type = ClickType.SWAP; break;
            case 3: type = ClickType.CLONE; break;
            case 4: type = ClickType.THROW; break;
            case 5: type = ClickType.QUICK_CRAFT; break;
            case 6: type = ClickType.PICKUP_ALL; break;
            default: return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid mode: " + mode);
        }

        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        Int2ObjectMap<ItemStack> changed = new Int2ObjectOpenHashMap<>();
        McVersionCompat.sendContainerClick(conn,
            windowId, stateId, slot, button, type,
            player.containerMenu.getCarried(), changed);
        return ActionResult.ok();
    }
}
