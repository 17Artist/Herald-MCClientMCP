package ai.herald.clientmod.platform;

import ai.herald.clientmod.platform.fabric.PlatformHelperImpl;

import java.nio.file.Path;

/**
 * Platform helper — delegates directly to Fabric implementation.
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
}
