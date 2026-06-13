package ai.herald.clientmod.action.container;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;

/** Stub — bundle_selected_slot requires Minecraft 1.21+, not supported on 1.20.1. */
public final class BundleSelectedSlotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        return ActionResult.error(ErrorCode.INVALID_PARAMS,
            "bundle_selected_slot requires Minecraft 1.21+; not supported on 1.20.1");
    }
}
