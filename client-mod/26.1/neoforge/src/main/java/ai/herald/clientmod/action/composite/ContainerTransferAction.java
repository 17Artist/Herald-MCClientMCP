package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Shift-click a slot — sends a QUICK_MOVE container click packet. */
public final class ContainerTransferAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        if (conn == null)   return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int windowId = JsonUtil.getIntOrDefault(params, "windowId", player.containerMenu.containerId);
        int stateId  = JsonUtil.getIntOrDefault(params, "stateId", player.containerMenu.getStateId());
        int slot     = JsonUtil.requireInt(params, "slot");
        int button   = JsonUtil.getIntOrDefault(params, "button", 0);

        McVersionCompat.sendContainerClick(conn,
            windowId,
            stateId,
            slot,
            button,
            ContainerInput.QUICK_MOVE,
            ItemStack.EMPTY,
            new Int2ObjectOpenHashMap<>());

        JsonObject data = new JsonObject();
        data.addProperty("window_id", windowId);
        data.addProperty("slot", slot);
        return ActionResult.ok(data);
    }
}
