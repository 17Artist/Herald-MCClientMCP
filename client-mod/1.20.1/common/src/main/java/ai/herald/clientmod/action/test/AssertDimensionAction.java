package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

/**
 * Asserts that the player is in the specified dimension.
 * Accepts short names (overworld, the_nether, the_end) or full ResourceLocation strings.
 */
public final class AssertDimensionAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        String expected = JsonUtil.requireString(params, "dimension");

        ResourceLocation dimLocation = level.dimension().location();
        String actual = dimLocation.toString();

        // Normalize short names
        String normalized = normalizeShort(expected);

        if (!actual.equals(normalized)) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected dimension " + normalized + " but got " + actual);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Player is in dimension " + actual);
        return ActionResult.ok(data);
    }

    private static String normalizeShort(String dim) {
        return switch (dim) {
            case "overworld" -> "minecraft:overworld";
            case "the_nether" -> "minecraft:the_nether";
            case "the_end" -> "minecraft:the_end";
            default -> dim.contains(":") ? dim : "minecraft:" + dim;
        };
    }
}
