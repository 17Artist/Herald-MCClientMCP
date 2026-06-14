package ai.herald.clientmod.action.block;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.DirectionUtil;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Place a block against the given face. Mirrors vanilla right-click-on-block:
 * {@link MultiPlayerGameMode#useItemOn}.
 */
public final class PlaceBlockAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gm = mc.gameMode;
        if (player == null || gm == null || mc.level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        Direction face = DirectionUtil.fromString(JsonUtil.getStringOrDefault(params, "face", "up"));
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));

        double cx = JsonUtil.getDoubleOrDefault(params, "cursorX", 0.5);
        double cy = JsonUtil.getDoubleOrDefault(params, "cursorY", 0.5);
        double cz = JsonUtil.getDoubleOrDefault(params, "cursorZ", 0.5);
        boolean insideBlock = JsonUtil.getBooleanOrDefault(params, "insideBlock", false);

        BlockPos pos = new BlockPos(x, y, z);
        Vec3 cursor = new Vec3(x + cx, y + cy, z + cz);
        BlockHitResult hit = new BlockHitResult(cursor, face, pos, insideBlock);

        InteractionResult result = gm.useItemOn(player, hand, hit);
        JsonObject data = new JsonObject();
        data.addProperty("result", result.toString());
        data.addProperty("consumed", result.consumesAction());
        return ActionResult.ok(data);
    }
}
