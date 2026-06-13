package ai.herald.clientmod.platform.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.function.Consumer;

public final class HeraldEventsImpl {

    private HeraldEventsImpl() {}

    public static void registerClientTick(Runnable task) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> task.run());
    }

    public static void registerFrameEnd(Runnable task) {
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> task.run());
    }

    public static void registerDisconnect(Runnable task) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> task.run());
    }

    public static void registerJoin(Runnable task) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> task.run());
    }

    public static void registerChatReceived(Consumer<String> consumer) {
        NeoForge.EVENT_BUS.addListener((ClientChatReceivedEvent e) -> {
            if (e.getMessage() != null) consumer.accept(e.getMessage().getString());
        });
    }

    public static void runOnClientThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
