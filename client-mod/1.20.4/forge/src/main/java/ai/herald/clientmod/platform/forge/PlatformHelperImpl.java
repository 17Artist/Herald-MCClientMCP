package ai.herald.clientmod.platform.forge;

import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class PlatformHelperImpl {

    private static final Logger LOG = LoggerFactory.getLogger("Herald-Platform");
    private PlatformHelperImpl() {}

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static String getLoaderName() {
        return "forge";
    }

    public static void scheduleHeadlessHide() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 60; i++) {
                    Thread.sleep(500);
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc != null && mc.getWindow() != null) {
                        long window = mc.getWindow().getWindow();
                        if (window != 0L) {
                            GLFW.glfwHideWindow(window);
                            LOG.info("Headless mode: window hidden (after {}ms)", (i + 1) * 500);
                            return;
                        }
                    }
                }
                LOG.warn("Headless: gave up waiting for window after 30s");
            } catch (Exception ex) {
                LOG.warn("Failed to hide window for headless mode", ex);
            }
        }, "Herald-HeadlessHide").start();
    }
}
