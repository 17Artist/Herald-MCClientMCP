package ai.herald.clientmod.platform;

import ai.herald.clientmod.platform.neoforge.PlatformHelperImpl;

import java.nio.file.Path;

/**
 * Platform helper — delegates directly to NeoForge implementation.
 */
public final class PlatformHelper {

    private PlatformHelper() {}

    /** The Minecraft game directory, e.g. {@code .minecraft/} on clients. */
    public static Path getGameDir() {
        return PlatformHelperImpl.getGameDir();
    }

    /** Which mod loader is running this build. */
    public static String getLoaderName() {
        return PlatformHelperImpl.getLoaderName();
    }

    /** Schedule window hide for headless mode (platform-specific thread safety). */
    public static void scheduleHeadlessHide() {
        PlatformHelperImpl.scheduleHeadlessHide();
    }
}
