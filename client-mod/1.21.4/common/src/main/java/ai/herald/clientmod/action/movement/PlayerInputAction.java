package ai.herald.clientmod.action.movement;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.InjectedInput;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

/**
 * Drives the local player's movement keys for one or more ticks by replacing
 * {@link LocalPlayer#input} with a controllable {@link InjectedInput}.
 *
 * <p>Params (all booleans, default false):
 * <ul>
 *   <li>{@code forward}, {@code backward}, {@code left}, {@code right}</li>
 *   <li>{@code jump}, {@code sneak}, {@code sprint}</li>
 *   <li>{@code release} — when true, restore the original {@link net.minecraft.client.player.KeyboardInput} and stop steering. All other booleans are ignored.</li>
 * </ul>
 *
 * <p>Numeric impulses {@code forwardImpulse}/{@code sidewaysImpulse} (-1.0..1.0)
 * may be supplied as overrides, otherwise they are derived from the booleans.
 *
 * <p>The {@link InjectedInput} instance is a per-process singleton that stays
 * installed across calls until {@code release:true} is sent, so successive
 * calls can mutate fields without re-installing.
 */
public final class PlayerInputAction implements ActionExecutor {

    private static final InjectedInput INJECTED = new InjectedInput();

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        boolean release = JsonUtil.getBooleanOrDefault(params, "release", false);
        if (release) {
            INJECTED.reset();
            INJECTED.uninstall(player);
            JsonObject data = new JsonObject();
            data.addProperty("released", true);
            return ActionResult.ok(data);
        }

        // (Re)install if the player's current input is something else.
        if (player.input != INJECTED) {
            INJECTED.reset();
            INJECTED.install(player);
        }

        boolean forward  = JsonUtil.getBooleanOrDefault(params, "forward",  false);
        boolean backward = JsonUtil.getBooleanOrDefault(params, "backward", false);
        boolean left     = JsonUtil.getBooleanOrDefault(params, "left",     false);
        boolean right    = JsonUtil.getBooleanOrDefault(params, "right",    false);
        boolean jump     = JsonUtil.getBooleanOrDefault(params, "jump",
                          JsonUtil.getBooleanOrDefault(params, "jumping", false));
        boolean sneak    = JsonUtil.getBooleanOrDefault(params, "sneak",
                          JsonUtil.getBooleanOrDefault(params, "sneaking", false));
        boolean sprint   = JsonUtil.getBooleanOrDefault(params, "sprint",   false);

        INJECTED.forward      = forward;
        INJECTED.backward     = backward;
        INJECTED.strafeLeft   = left;
        INJECTED.strafeRight  = right;
        INJECTED.jumping      = jump;
        INJECTED.shiftKeyDown = sneak;
        INJECTED.sprinting    = sprint;
        // Sprint flag must be applied to the player too; LocalPlayer.aiStep
        // toggles it back off if not held, but we want it sticky until released.
        if (sprint && !player.isSprinting()) player.setSprinting(true);
        if (!sprint && player.isSprinting() && !forward) player.setSprinting(false);

        JsonObject data = new JsonObject();
        data.addProperty("installed", true);
        data.addProperty("forward",   forward);
        data.addProperty("backward",  backward);
        data.addProperty("left",      left);
        data.addProperty("right",     right);
        data.addProperty("jump",      jump);
        data.addProperty("sneak",     sneak);
        data.addProperty("sprint",    sprint);
        return ActionResult.ok(data);
    }
}
