package ai.herald.clientmod.platform.neoforge;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class PlatformHelperImpl {

    private PlatformHelperImpl() {}

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static String getLoaderName() {
        return "neoforge";
    }
}
