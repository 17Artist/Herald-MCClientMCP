package ai.herald.clientmod.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Architectury-resolved impl of {@link ai.herald.clientmod.platform.PlatformHelper}.
 * Class name + package is significant — do not rename.
 */
public final class PlatformHelperImpl {

    private static final Logger LOG = LoggerFactory.getLogger("Herald-Platform");
    private PlatformHelperImpl() {}

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static String getLoaderName() {
        return "fabric";
    }

    public static void scheduleHeadlessHide() {
        // Fabric init runs on render thread so direct call works
        try {
            long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
            if (window != 0L) {
                GLFW.glfwHideWindow(window);
                LOG.info("Headless mode: window hidden");
            }
        } catch (Exception e) {
            LOG.warn("Failed to hide window for headless mode", e);
        }
    }
}
