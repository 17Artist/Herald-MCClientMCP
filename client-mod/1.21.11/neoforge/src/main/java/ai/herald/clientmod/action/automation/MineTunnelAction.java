package ai.herald.clientmod.action.automation;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sync: mines a tunnel in a given direction using /fill start end air.
 * Direction: north=z--, south=z++, east=x++, west=x--.
 */
public final class MineTunnelAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        String direction = JsonUtil.requireString(params, "direction").toLowerCase();
        int length = JsonUtil.requireInt(params, "length");
        int width = JsonUtil.getIntOrDefault(params, "width", 1);
        int height = JsonUtil.getIntOrDefault(params, "height", 3);

        if (length < 1) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "length must be >= 1");
        }

        // Calculate end position based on direction
        int endX = x, endZ = z;
        int widthOffsetX = 0, widthOffsetZ = 0;
        switch (direction) {
            case "north":
                endZ = z - (length - 1);
                widthOffsetX = width - 1;
                break;
            case "south":
                endZ = z + (length - 1);
                widthOffsetX = width - 1;
                break;
            case "east":
                endX = x + (length - 1);
                widthOffsetZ = width - 1;
                break;
            case "west":
                endX = x - (length - 1);
                widthOffsetZ = width - 1;
                break;
            default:
                return ActionResult.error(ErrorCode.INVALID_PARAMS,
                    "direction must be north/south/east/west, got: " + direction);
        }

        int minX = Math.min(x, endX);
        int maxX = Math.max(x, endX) + widthOffsetX;
        int minZ = Math.min(z, endZ);
        int maxZ = Math.max(z, endZ) + widthOffsetZ;
        int minY = y;
        int maxY = y + height - 1;

        player.connection.sendCommand(
            "fill " + minX + " " + minY + " " + minZ + " " + maxX + " " + maxY + " " + maxZ + " air");

        JsonObject data = new JsonObject();
        data.addProperty("direction", direction);
        data.addProperty("length", length);
        data.addProperty("width", width);
        data.addProperty("height", height);
        long volume = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        data.addProperty("volume", volume);
        return ActionResult.ok(data);
    }
}
