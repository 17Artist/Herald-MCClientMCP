package ai.herald.clientmod.util;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;

/**
 * {@link Input} subclass whose {@link #tick(boolean, float)} publishes
 * caller-supplied booleans instead of reading the real keyboard.
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
 *
 * <p>Port of {@code BlackBoxPro fabric/util/InjectedInput.kt} to Java +
 * Mojang 1.20.1 names ({@code pressingForward} → {@code up}, etc).
 */
public final class InjectedInput extends Input {

    public boolean forward;
    public boolean backward;
    public boolean strafeLeft;
    public boolean strafeRight;
    public boolean sprinting;

    private Input original;
    private LocalPlayer installedPlayer;

    @Override
    public void tick(boolean slowDown, float slowDownFactor) {
        this.up = forward;
        this.down = backward;
        this.left = strafeLeft;
        this.right = strafeRight;
        this.forwardImpulse = (forward == backward) ? 0.0F : (forward ? 1.0F : -1.0F);
        this.leftImpulse    = (strafeLeft == strafeRight) ? 0.0F : (strafeLeft ? 1.0F : -1.0F);
        // jumping / shiftKeyDown are set directly by the caller on this instance.
        if (slowDown) {
            this.leftImpulse *= 0.3F;
            this.forwardImpulse *= 0.3F;
        }
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
    }
}
