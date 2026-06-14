package ai.herald.clientmod.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class PlatformHelper {
    private PlatformHelper() {}

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static String getLoaderName() {
        return "fabric";
    }
}
