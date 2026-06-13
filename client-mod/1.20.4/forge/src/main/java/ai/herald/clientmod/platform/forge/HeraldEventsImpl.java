package ai.herald.clientmod.platform.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.function.Consumer;

public final class HeraldEventsImpl {

    private HeraldEventsImpl() {}

    public static void registerClientTick(Runnable task) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) task.run();
        });
    }

    public static void registerFrameEnd(Runnable task) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) task.run();
        });
    }

    public static void registerDisconnect(Runnable task) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> task.run());
    }

    public static void registerJoin(Runnable task) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> task.run());
    }

    public static void registerChatReceived(Consumer<String> consumer) {
        MinecraftForge.EVENT_BUS.addListener((ClientChatReceivedEvent e) -> {
            if (e.getMessage() != null) consumer.accept(e.getMessage().getString());
        });
    }

    public static void runOnClientThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
