package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

public class WorldCreateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String name = JsonUtil.getStringOrDefault(params, "name", null);

        if (name == null || name.isEmpty()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "name is required");
        }

        JsonObject data = new JsonObject();
        data.addProperty("implemented", false);
        data.addProperty("name", name);
        data.addProperty("note", "World creation requires GUI screen manipulation. Use gui_click_widget to navigate the Create World screen, or use server commands for server-side world management.");
        return ActionResult.ok(data);
    }
}
