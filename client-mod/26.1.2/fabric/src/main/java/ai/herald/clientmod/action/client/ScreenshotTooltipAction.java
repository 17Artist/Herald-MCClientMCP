package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;

/** Stub — tooltip-overlay screenshot helper not ported to Herald yet. */
public final class ScreenshotTooltipAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        return ActionResult.error(ErrorCode.INVALID_PARAMS,
            "screenshot_tooltip is not supported: container-tooltip render bridge not ported to Herald");
    }
}
