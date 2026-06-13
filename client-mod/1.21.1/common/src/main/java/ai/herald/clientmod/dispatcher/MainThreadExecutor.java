package ai.herald.clientmod.dispatcher;

/** Hands a {@link Runnable} off to the Minecraft client thread. */
@FunctionalInterface
public interface MainThreadExecutor {
    void execute(Runnable task);
}
