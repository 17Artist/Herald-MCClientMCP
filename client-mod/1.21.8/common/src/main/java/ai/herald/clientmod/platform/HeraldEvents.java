package ai.herald.clientmod.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.Consumer;

/**
 * Wires loader-specific event buses to Herald callbacks.
 * Implementations live in {@code fabric/}, {@code forge/}, {@code neoforge/} modules.
 */
public final class HeraldEvents {

    private HeraldEvents() {}

    /** Register a task to run at the end of every client tick. */
    @ExpectPlatform
    public static void registerClientTick(Runnable task) {
        throw new AssertionError("HeraldEvents.registerClientTick not bound by Architectury");
    }

    /** Register a task to run at the end of every rendered frame (60+ fps). */
    @ExpectPlatform
    public static void registerFrameEnd(Runnable task) {
        throw new AssertionError("HeraldEvents.registerFrameEnd not bound by Architectury");
    }

    /** Register a task to run when the local player disconnects from any world/server. */
    @ExpectPlatform
    public static void registerDisconnect(Runnable task) {
        throw new AssertionError("HeraldEvents.registerDisconnect not bound by Architectury");
    }

    /** Register a task to run when the local player joins a world / server. */
    @ExpectPlatform
    public static void registerJoin(Runnable task) {
        throw new AssertionError("HeraldEvents.registerJoin not bound by Architectury");
    }

    /** Register a consumer of incoming chat / system messages (plain text). */
    @ExpectPlatform
    public static void registerChatReceived(Consumer<String> consumer) {
        throw new AssertionError("HeraldEvents.registerChatReceived not bound by Architectury");
    }

    /** Hand a task off to the Minecraft client thread (safe from any thread). */
    @ExpectPlatform
    public static void runOnClientThread(Runnable task) {
        throw new AssertionError("HeraldEvents.runOnClientThread not bound by Architectury");
    }
}
