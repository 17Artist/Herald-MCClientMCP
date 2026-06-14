package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Port-equivalent of BlackBoxPro QueryTooltipStateAction.
 *
 * <p>Reads {@code Options.advancedItemTooltips} (public field on 1.20.1) — the
 * F3+H toggle for advanced item tooltips.</p>
 */
public final class QueryTooltipStateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        Options options = mc.options;
        boolean advanced = options != null && options.advancedItemTooltips;

        JsonObject data = new JsonObject();
        data.addProperty("advanced", advanced);
        return ActionResult.ok(data);
    }
}
