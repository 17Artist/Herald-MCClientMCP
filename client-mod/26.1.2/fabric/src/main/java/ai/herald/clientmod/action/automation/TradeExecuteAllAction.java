package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillStatus;
import ai.herald.clientmod.skill.SkillTask;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import ai.herald.clientmod.util.McVersionCompat;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Async: repeatedly executes a trade until stock runs out.
 * One trade per tick to avoid overwhelming the server.
 */
public final class TradeExecuteAllAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int offerIndex = JsonUtil.requireInt(params, "offerIndex");

        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "No merchant screen open");
        }

        MerchantOffers offers = merchantMenu.getOffers();
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "offerIndex out of range: " + offerIndex);
        }

        SkillEngine engine = HeraldClientMod.skillEngine();
        SkillTask task = engine.create("trade_execute_all", params);

        HeraldClientMod.tickScheduler().schedule(1, new TradeRepeater(task.taskId(), offerIndex));
        return ActionResult.async(task.taskId());
    }

    private static final class TradeRepeater implements Runnable {
        private final String taskId;
        private final int offerIndex;
        private int executed = 0;

        TradeRepeater(String taskId, int offerIndex) {
            this.taskId = taskId;
            this.offerIndex = offerIndex;
        }

        @Override
        public void run() {
            SkillEngine engine = HeraldClientMod.skillEngine();
            SkillTask task = engine.get(taskId);
            if (task == null || task.status() != SkillStatus.RUNNING) return;

            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) { engine.fail(taskId, "Player left world"); return; }

            if (!(p.containerMenu instanceof MerchantMenu merchantMenu)) {
                JsonObject result = new JsonObject();
                result.addProperty("executed", executed);
                result.addProperty("reason", "merchant_screen_closed");
                engine.complete(taskId, result);
                return;
            }

            MerchantOffers offers = merchantMenu.getOffers();
            if (offerIndex >= offers.size()) {
                JsonObject result = new JsonObject();
                result.addProperty("executed", executed);
                result.addProperty("reason", "offer_unavailable");
                engine.complete(taskId, result);
                return;
            }

            MerchantOffer offer = offers.get(offerIndex);
            if (offer.isOutOfStock()) {
                JsonObject result = new JsonObject();
                result.addProperty("executed", executed);
                result.addProperty("reason", "out_of_stock");
                engine.complete(taskId, result);
                return;
            }

            // Select and execute trade
            ClientPacketListener conn = p.connection;
            conn.send(new ServerboundSelectTradePacket(offerIndex));
            McVersionCompat.sendContainerClick(conn,
                merchantMenu.containerId,
                merchantMenu.getStateId(),
                2, // result slot
                0,
                ContainerInput.QUICK_MOVE,
                ItemStack.EMPTY,
                new Int2ObjectOpenHashMap<>());
            executed++;

            // Schedule next tick
            HeraldClientMod.tickScheduler().schedule(1, this);
        }
    }
}