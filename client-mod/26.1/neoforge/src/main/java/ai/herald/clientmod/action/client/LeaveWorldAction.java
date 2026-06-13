package ai.herald.clientmod.action.client;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** Port of BlackBoxPro client/LeaveWorldAction.kt — simplified sync disconnect. */
public final class LeaveWorldAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level != null) {
                mc.level.disconnect(Component.empty());
            }
            mc.disconnect(new TitleScreen(), true);
        });
        return ActionResult.ok();
    }
}
