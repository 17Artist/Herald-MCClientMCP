package ai.herald.clientmod.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * Platform-agnostic probes resolved at runtime by loader-specific {@code PlatformHelperImpl}
 * classes via Architectury's {@code @ExpectPlatform} contract.
 */
public final class PlatformHelper {

    private PlatformHelper() {}

    /** The Minecraft game directory, e.g. {@code .minecraft/} on clients. */
    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError("PlatformHelper.getGameDir not bound by Architectury");
    }

    /** Which mod loader is running this build: {@code "fabric"}, {@code "forge"}, or {@code "neoforge"}. */
    @ExpectPlatform
    public static String getLoaderName() {
        throw new AssertionError("PlatformHelper.getLoaderName not bound by Architectury");
    }
}
