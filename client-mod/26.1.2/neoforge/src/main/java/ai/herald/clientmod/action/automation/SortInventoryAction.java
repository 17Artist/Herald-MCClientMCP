package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * sort_inventory — sorts player inventory items.
 * Params: strategy?(alphabetical/type/stack, default type)
 *
 * Currently returns NOT_IMPLEMENTED as inventory sorting requires
 * multi-tick container click sequences that need careful state tracking.
 * Future implementation will group items by type and merge partial stacks.
 */
public final class SortInventoryAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String strategy = JsonUtil.getStringOrDefault(params, "strategy", "type");

        // Validate strategy parameter
        if (!"alphabetical".equals(strategy) && !"type".equals(strategy) && !"stack".equals(strategy)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "Invalid strategy: " + strategy + ". Use: alphabetical, type, or stack");
        }

        return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "sort_inventory is not yet implemented. Sorting strategy '" + strategy
                        + "' requires multi-tick container click sequences. "
                        + "Use move_items to manually rearrange slots, or use "
                        + "click_slot with QUICK_MOVE mode for shift-clicking.");
    }
}
