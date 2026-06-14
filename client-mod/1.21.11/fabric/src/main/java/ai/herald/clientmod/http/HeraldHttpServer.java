package ai.herald.clientmod.http;

import ai.herald.clientmod.protocol.HttpEndpoints;
import ai.herald.clientmod.util.HeraldLogger;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the JDK-builtin {@link HttpServer}. Mounts all handlers under a
 * single {@link TokenFilter}. Binds to {@code 127.0.0.1} only — never
 * exposes to LAN.
 */
public final class HeraldHttpServer {

    private static final Logger LOG = HeraldLogger.of(HeraldHttpServer.class);

    private final int port;
    private final String token;
    private final ApiRouter apiRouter;
    private final SkillRouter skillRouter;
    private final EventsHandler eventsHandler;
    private final List<ExtraHandler> extraHandlers = new ArrayList<>();

    private HttpServer server;

    private static final class ExtraHandler {
        final String path;
        final com.sun.net.httpserver.HttpHandler handler;
        ExtraHandler(String path, com.sun.net.httpserver.HttpHandler handler) {
            this.path = path;
            this.handler = handler;
        }
    }

    public HeraldHttpServer(int port,
                            String token,
                            ApiRouter apiRouter,
                            SkillRouter skillRouter,
                            EventsHandler eventsHandler) {
        this.port = port;
        this.token = token;
        this.apiRouter = apiRouter;
        this.skillRouter = skillRouter;
        this.eventsHandler = eventsHandler;
    }

    public synchronized void start() throws IOException {
        if (server != null) throw new IllegalStateException("Already started");
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        TokenFilter auth = new TokenFilter(token);

        mount(s, HttpEndpoints.PING, apiRouter, auth);
        mount(s, HttpEndpoints.ACTIONS, apiRouter, auth);
        mount(s, HttpEndpoints.ACTION_PREFIX, apiRouter, auth);
        mount(s, HttpEndpoints.SKILL_PREFIX, skillRouter, auth);
        mount(s, HttpEndpoints.EVENTS, eventsHandler, auth);
        mount(s, HttpEndpoints.SHUTDOWN, apiRouter, auth);

        // Mount additional handlers (e.g. stream)
        LOG.info("Registering {} extra handlers", extraHandlers.size());
        for (var entry : extraHandlers) {
            s.createContext(entry.path, entry.handler);
            LOG.info("  Mounted: {}", entry.path);
        }

        s.setExecutor(Executors.newCachedThreadPool(daemonFactory("Herald-Http")));
        s.start();
        this.server = s;
        LOG.info("Herald HTTP server listening on 127.0.0.1:{}", port);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOG.info("Herald HTTP server stopped");
        }
    }

    public int port() {
        return port;
    }

    /**
     * Add an extra handler (no auth). Must be called before start().
     */
    public void addHandler(String path, com.sun.net.httpserver.HttpHandler handler) {
        extraHandlers.add(new ExtraHandler(path, handler));
    }

    public boolean isRunning() {
        return server != null;
    }

    private static void mount(HttpServer s, String path,
                              com.sun.net.httpserver.HttpHandler handler,
                              TokenFilter filter) {
        HttpContext ctx = s.createContext(path, handler);
        ctx.getFilters().add(filter);
    }

    private static ThreadFactory daemonFactory(String name) {
        AtomicInteger counter = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, name + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
