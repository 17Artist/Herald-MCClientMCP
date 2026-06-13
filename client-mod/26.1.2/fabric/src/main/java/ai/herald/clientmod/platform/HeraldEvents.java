package ai.herald.clientmod.platform;

import ai.herald.clientmod.platform.fabric.HeraldEventsImpl;

import java.util.function.Consumer;

/**
 * Wires Fabric event buses to Herald callbacks.
 * Delegates directly to the Fabric implementation.
 */
public final class HeraldEvents {

    private HeraldEvents() {}

    /** Register a task to run at the end of every client tick. */
    public static void registerClientTick(Runnable task) {
        HeraldEventsImpl.registerClientTick(task);
    }

    /** Register a task to run at the end of every rendered frame (60+ fps). */
    public static void registerFrameEnd(Runnable task) {
        HeraldEventsImpl.registerFrameEnd(task);
    }

    /** Register a task to run when the local player disconnects from any world/server. */
    public static void registerDisconnect(Runnable task) {
        HeraldEventsImpl.registerDisconnect(task);
    }

    /** Register a task to run when the local player joins a world / server. */
    public static void registerJoin(Runnable task) {
        HeraldEventsImpl.registerJoin(task);
    }

    /** Register a consumer of incoming chat / system messages (plain text). */
    public static void registerChatReceived(Consumer<String> consumer) {
        HeraldEventsImpl.registerChatReceived(consumer);
    }

    /** Hand a task off to the Minecraft client thread (safe from any thread). */
    public static void runOnClientThread(Runnable task) {
        HeraldEventsImpl.runOnClientThread(task);
    }
}
