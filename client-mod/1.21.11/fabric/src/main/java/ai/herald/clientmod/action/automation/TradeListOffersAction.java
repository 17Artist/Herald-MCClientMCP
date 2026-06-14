package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Sync: reads all trade offers from the currently open merchant screen.
 * Returns offer details including cost, result, uses, and disabled state.
 */
public final class TradeListOffersAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "No merchant screen open");
        }

        MerchantOffers offers = merchantMenu.getOffers();
        JsonArray offerArray = new JsonArray();

        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            JsonObject entry = new JsonObject();
            entry.addProperty("index", i);

            // Cost A
            ItemStack costA = offer.getCostA();
            entry.addProperty("costA_item", itemName(costA));
            entry.addProperty("costA_count", costA.getCount());

            // Cost B (optional)
            ItemStack costB = offer.getCostB();
            if (!costB.isEmpty()) {
                entry.addProperty("costB_item", itemName(costB));
                entry.addProperty("costB_count", costB.getCount());
            }

            // Result
            ItemStack result = offer.getResult();
            entry.addProperty("result_item", itemName(result));
            entry.addProperty("result_count", result.getCount());

            entry.addProperty("uses", offer.getUses());
            entry.addProperty("maxUses", offer.getMaxUses());
            entry.addProperty("disabled", offer.isOutOfStock());

            offerArray.add(entry);
        }

        JsonObject data = new JsonObject();
        data.addProperty("offerCount", offers.size());
        data.add("offers", offerArray);
        return ActionResult.ok(data);
    }

    private static String itemName(ItemStack stack) {
        if (stack.isEmpty()) return "air";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : "unknown";
    }
}
