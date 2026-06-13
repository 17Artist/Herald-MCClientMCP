package ai.herald.clientmod.action.container;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import com.google.gson.JsonObject;

/** Port of BlackBoxPro container/HoverSlotAction.kt — stub (tooltip helper not ported). */
public final class HoverSlotAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        return ActionResult.ok();
    }
}
