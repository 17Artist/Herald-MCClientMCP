package ai.herald.clientmod.platform.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class HeraldEventsImpl {

    private HeraldEventsImpl() {}

    public static void registerClientTick(Runnable task) {
        ClientTickEvents.END_CLIENT_TICK.register((Minecraft mc) -> task.run());
    }

    public static void registerFrameEnd(Runnable task) {
        // WorldRenderEvents.END was removed in Fabric API for MC 26.1.
        // Fall back to end-of-tick which is functionally equivalent for our purposes.
        ClientTickEvents.END_CLIENT_TICK.register((Minecraft mc) -> task.run());
    }

    public static void registerDisconnect(Runnable task) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> task.run());
    }

    public static void registerJoin(Runnable task) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> task.run());
    }

    public static void registerChatReceived(Consumer<String> consumer) {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) consumer.accept(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            consumer.accept(message.getString())
        );
    }

    public static void runOnClientThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
