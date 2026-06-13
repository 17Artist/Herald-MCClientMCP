package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.ItemStackSerializer;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Port of BlackBoxPro QueryHeldItemAction.kt to Java + Mojang 1.20.1. */
public final class QueryHeldItemAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String handStr = JsonUtil.getStringOrDefault(params, "hand", "main_hand");
        InteractionHand hand = HandUtil.fromString(handStr);

        ItemStack stack = player.getItemInHand(hand);
        JsonObject data = ItemStackSerializer.serialize(stack);
        return ActionResult.ok(data);
    }
}
