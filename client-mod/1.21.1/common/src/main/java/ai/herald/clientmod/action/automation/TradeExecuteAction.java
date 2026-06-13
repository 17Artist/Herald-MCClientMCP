package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Sync: executes a trade by selecting the offer and shift-clicking the result slot.
 * Requires merchant screen to be open.
 */
public final class TradeExecuteAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        ClientPacketListener conn = McHelper.connection();
        if (player == null || conn == null) return McHelper.notInGame();

        int offerIndex = JsonUtil.requireInt(params, "offerIndex");
        int count = JsonUtil.getIntOrDefault(params, "count", 1);

        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "No merchant screen open");
        }

        MerchantOffers offers = merchantMenu.getOffers();
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "offerIndex out of range: " + offerIndex + " (offers: " + offers.size() + ")");
        }

        MerchantOffer offer = offers.get(offerIndex);
        if (offer.isOutOfStock()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Trade offer is out of stock");
        }

        int executed = 0;
        for (int i = 0; i < count; i++) {
            if (offer.isOutOfStock()) break;

            // Select the trade offer
            conn.send(new ServerboundSelectTradePacket(offerIndex));

            // Shift-click result slot (slot 2 in merchant menu)
            int containerId = merchantMenu.containerId;
            int resultSlot = 2;
            McVersionCompat.sendContainerClick(conn,
                containerId,
                merchantMenu.getStateId(),
                resultSlot,
                0,
                ClickType.QUICK_MOVE,
                ItemStack.EMPTY,
                new Int2ObjectOpenHashMap<>());
            executed++;
        }

        JsonObject data = new JsonObject();
        data.addProperty("executed", executed);
        data.addProperty("offerIndex", offerIndex);
        return ActionResult.ok(data);
    }
}
