package ai.herald.clientmod.util;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * {@link ClientInput} subclass whose {@link #tick()} publishes
 * caller-supplied booleans instead of reading the real keyboard.
 *
 * <p>In 1.21.4, ClientInput stores movement state in a {@code keyPresses}
 * ({@link Input} record) plus {@code forwardImpulse}/{@code leftImpulse}.
 * The {@code tick()} method has no parameters.
 *
 * <p>Usage:
 * <pre>{@code
 *   InjectedInput injected = new InjectedInput();
 *   injected.install(player);
 *   injected.forward = true;
 *   // … tick(s) later …
 *   injected.reset();
 *   injected.uninstall();
 * }</pre>
 */
public final class InjectedInput extends ClientInput {

    public boolean forward;
    public boolean backward;
    public boolean strafeLeft;
    public boolean strafeRight;
    public boolean jumping;
    public boolean shiftKeyDown;
    public boolean sprinting;

    private ClientInput original;
    private LocalPlayer installedPlayer;

    @Override
    public void tick() {
        this.keyPresses = new Input(forward, backward, strafeLeft, strafeRight, jumping, shiftKeyDown, sprinting);
        this.forwardImpulse = (forward == backward) ? 0.0F : (forward ? 1.0F : -1.0F);
        this.leftImpulse    = (strafeLeft == strafeRight) ? 0.0F : (strafeLeft ? 1.0F : -1.0F);
    }

    public void install(LocalPlayer player) {
        this.original = player.input;
        this.installedPlayer = player;
        player.input = this;
    }

    public void uninstall() {
        uninstall(null);
    }

    public void uninstall(LocalPlayer target) {
        LocalPlayer p = target != null ? target : installedPlayer;
        if (original != null && p != null && p.input == this) {
            p.input = original;
        }
        original = null;
        installedPlayer = null;
    }

    public void reset() {
        forward = backward = strafeLeft = strafeRight = sprinting = false;
        jumping = false;
        shiftKeyDown = false;
        forwardImpulse = 0.0F;
        leftImpulse = 0.0F;
        this.keyPresses = Input.EMPTY;
    }
}
