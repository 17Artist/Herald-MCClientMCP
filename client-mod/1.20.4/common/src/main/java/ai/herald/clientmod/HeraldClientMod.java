package ai.herald.clientmod;

import ai.herald.clientmod.action.ActionRegistration;
import ai.herald.clientmod.catalog.ActionCatalog;
import ai.herald.clientmod.dispatcher.ActionRegistry;
import ai.herald.clientmod.dispatcher.CommandDispatcher;
import ai.herald.clientmod.dispatcher.ResponseFutureRegistry;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.events.EventBus;
import ai.herald.clientmod.events.GameEventBridge;
import ai.herald.clientmod.http.ApiRouter;
import ai.herald.clientmod.http.EventsHandler;
import ai.herald.clientmod.http.HeraldHttpServer;
import ai.herald.clientmod.http.PortPicker;
import ai.herald.clientmod.http.SkillRouter;
import ai.herald.clientmod.http.TokenStore;
import ai.herald.clientmod.platform.HeraldEvents;
import ai.herald.clientmod.platform.PlatformHelper;
import ai.herald.clientmod.scheduler.TickScheduler;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.stream.RemoteInputHandler;
import ai.herald.clientmod.stream.StreamBridge;
import ai.herald.clientmod.stream.StreamInputHandler;
import ai.herald.clientmod.stream.StreamServer;
import ai.herald.clientmod.util.HeraldLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mod lifecycle entry. Each loader's platform initializer calls
 * {@link #init()} once, on the client thread, after Minecraft is ready.
 */
public final class HeraldClientMod {

    private static final Logger LOG = HeraldLogger.of(HeraldClientMod.class);

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private static ActionRegistry registry;
    private static ResponseFutureRegistry futures;
    private static CommandDispatcher dispatcher;
    private static SkillEngine skillEngine;
    private static EventBus eventBus;
    private static ChatHistoryBuffer chatHistory;
    private static TickScheduler tickScheduler;
    private static HeraldHttpServer server;
    private static TokenStore tokenStore;
    private static Path portFile;
    private static StreamBridge streamBridge;
    private static StreamServer streamServer;

    private HeraldClientMod() {}

    public static void init() {
        if (!STARTED.compareAndSet(false, true)) {
            LOG.warn("HeraldClientMod.init() called twice; ignoring");
            return;
        }

        // Hide window if headless mode requested
        if ("true".equals(System.getProperty("herald.headless"))) {
            PlatformHelper.scheduleHeadlessHide();
        }

        try {
            String loader = PlatformHelper.getLoaderName();
            LOG.info("Booting {} {} on {} (MC {})",
                HeraldConstants.MOD_NAME, HeraldConstants.MOD_VERSION,
                loader, HeraldConstants.MC_VERSION);

            registry = new ActionRegistry();
            futures = new ResponseFutureRegistry();
            skillEngine = new SkillEngine();
            eventBus = new EventBus();
            chatHistory = new ChatHistoryBuffer();
            tickScheduler = new TickScheduler();

            ActionRegistration.registerAll(registry);
            registry.freeze(ActionCatalog.ids());

            dispatcher = new CommandDispatcher(registry, futures, HeraldEvents::runOnClientThread);

            ApiRouter apiRouter = new ApiRouter(registry, dispatcher, futures, loader, HeraldClientMod::shutdown);
            SkillRouter skillRouter = new SkillRouter(skillEngine);
            EventsHandler eventsHandler = new EventsHandler(eventBus);

            // Stream server for headless mode (frame push + input receive)
            streamServer = new StreamServer(new RemoteInputHandler());
            streamBridge = new StreamBridge(streamServer);

            Path heraldDir = PlatformHelper.getGameDir().resolve(HeraldConstants.STATE_DIR_NAME);
            Files.createDirectories(heraldDir);
            tokenStore = TokenStore.boot(heraldDir);

            int port = PortPicker.pickDefault();
            portFile = heraldDir.resolve(HeraldConstants.PORT_FILE_NAME);
            Files.writeString(portFile, port + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            server = new HeraldHttpServer(port, tokenStore.token(), apiRouter, skillRouter, eventsHandler);
            server.addHandler("/stream", streamServer);
            server.addHandler("/stream/input", new StreamInputHandler(new RemoteInputHandler()));
            LOG.info("Added stream handlers, starting HTTP server...");
            server.start();

            HeraldEvents.registerClientTick(tickScheduler::tick);
            HeraldEvents.registerClientTick(streamBridge::onFrameRendered);
            HeraldEvents.registerDisconnect(() -> {
                int cleared = tickScheduler.clear();
                if (cleared > 0) LOG.info("Cleared {} pending tick tasks on disconnect", cleared);
            });

            // Wire game-state events → /events SSE
            new GameEventBridge(eventBus, chatHistory).install();

            Runtime.getRuntime().addShutdownHook(new Thread(HeraldClientMod::shutdown, "Herald-Shutdown"));

            LOG.info("[Herald-Client] ready on 127.0.0.1:{} loader={} mc={} actions={}",
                port, loader, HeraldConstants.MC_VERSION, registry.size());
        } catch (Throwable e) {
            LOG.error("Failed to boot Herald: {}", e.toString(), e);
            STARTED.set(false);
        }
    }

    public static synchronized void shutdown() {
        boolean shouldStopMc = server != null; // 如果 server 还在说明是 API 主动触发
        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } catch (Throwable t) {
            LOG.warn("Error stopping HTTP server: {}", t.toString());
        }
        try {
            if (portFile != null) {
                Files.deleteIfExists(portFile);
                portFile = null;
            }
        } catch (Throwable t) {
            LOG.warn("Error deleting port file: {}", t.toString());
        }
        try {
            if (tokenStore != null) {
                tokenStore.cleanup();
                tokenStore = null;
            }
        } catch (Throwable t) {
            LOG.warn("Error deleting token file: {}", t.toString());
        }
        // 如果是 API 触发的 shutdown（不是 JVM shutdown hook），则退出 JVM
        if (shouldStopMc) {
            LOG.info("API shutdown triggered, halting JVM in 1 second...");
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                Runtime.getRuntime().halt(0);
            }, "Herald-Halt").start();
        }
    }

    // === Accessors used by actions on the client thread ===

    public static TickScheduler tickScheduler() {
        if (tickScheduler == null) {
            throw new IllegalStateException("HeraldClientMod not initialised");
        }
        return tickScheduler;
    }

    public static SkillEngine skillEngine() {
        if (skillEngine == null) {
            throw new IllegalStateException("HeraldClientMod not initialised");
        }
        return skillEngine;
    }

    public static EventBus eventBus() {
        if (eventBus == null) {
            throw new IllegalStateException("HeraldClientMod not initialised");
        }
        return eventBus;
    }

    public static ChatHistoryBuffer chatHistory() {
        if (chatHistory == null) {
            throw new IllegalStateException("HeraldClientMod not initialised");
        }
        return chatHistory;
    }

    public static CommandDispatcher dispatcher() {
        return dispatcher;
    }
}
