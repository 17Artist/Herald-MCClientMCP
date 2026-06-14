package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;

/** Stub — creating worlds programmatically is not supported in this build. */
public final class CreateWorldAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        return ActionResult.error(ErrorCode.INVALID_PARAMS,
            "create_world is not supported: async world-creation runtime not ported to Herald");
    }
}
