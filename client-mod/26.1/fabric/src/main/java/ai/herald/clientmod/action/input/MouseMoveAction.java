package ai.herald.clientmod.action.input;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Simulates mouse movement (relative camera rotation).
 * Params:
 *   dx (required): horizontal mouse delta (positive = look right)
 *   dy (required): vertical mouse delta (positive = look down)
 */
public final class MouseMoveAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        if (!params.has("dx") || !params.has("dy")) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required params: dx, dy");
        }
        double dx = params.get("dx").getAsDouble();
        double dy = params.get("dy").getAsDouble();

        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        Minecraft mc = McHelper.mc();
        mc.execute(() -> {
            // Simulate mouse movement as camera rotation
            // MC sensitivity: 1 pixel of mouse movement ≈ 0.15 degrees
            player.turn(dx, dy);
        });

        JsonObject data = new JsonObject();
        data.addProperty("dx", dx);
        data.addProperty("dy", dy);
        data.addProperty("yaw", player.getYRot());
        data.addProperty("pitch", player.getXRot());
        return ActionResult.ok(data);
    }
}
