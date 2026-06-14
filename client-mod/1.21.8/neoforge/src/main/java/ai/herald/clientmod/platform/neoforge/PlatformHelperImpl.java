package ai.herald.clientmod.platform.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlatformHelperImpl {

    private static final Logger LOG = LoggerFactory.getLogger("Herald-Platform");
    private PlatformHelperImpl() {}

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static String getLoaderName() {
        return "neoforge";
    }

    public static void scheduleHeadlessHide() {
        final AtomicBoolean done = new AtomicBoolean(false);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> {
            if (done.compareAndSet(false, true)) {
                try {
                    long window = Minecraft.getInstance().getWindow().getWindow();
                    if (window != 0L) {
                        GLFW.glfwHideWindow(window);
                        LOG.info("Headless mode: window hidden");
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to hide window for headless mode", ex);
                }
            }
        });
    }
}
