package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;

/** Stub — debug_sample_subscription requires Minecraft 1.20.5+, not supported on 1.20.1. */
public final class DebugSampleAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        return ActionResult.error(ErrorCode.INVALID_PARAMS,
            "debug_sample_subscription requires Minecraft 1.20.5+; not supported on 1.20.1");
    }
}
