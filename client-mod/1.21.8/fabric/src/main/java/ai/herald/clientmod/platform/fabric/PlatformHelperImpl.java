package ai.herald.clientmod.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Architectury-resolved impl of {@link ai.herald.clientmod.platform.PlatformHelper}.
 * Class name + package is significant — do not rename.
 */
public final class PlatformHelperImpl {

    private PlatformHelperImpl() {}

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static String getLoaderName() {
        return "fabric";
    }
}
