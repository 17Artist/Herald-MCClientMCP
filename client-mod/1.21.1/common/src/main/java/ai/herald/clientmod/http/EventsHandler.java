package ai.herald.clientmod.http;

import ai.herald.clientmod.events.EventBus;
import ai.herald.clientmod.events.HeraldEvent;
import ai.herald.clientmod.util.HeraldLogger;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Server-Sent-Events stream at {@code GET /events}.
 *
 * <p>Phase 1 ships a heartbeat every 15s plus whatever {@link EventBus}
 * subscribers publish (zero producers in Phase 1). The connection lives
 * until the client disconnects or the server stops.
 */
public final class EventsHandler implements HttpHandler {

    private static final Logger LOG = HeraldLogger.of(EventsHandler.class);
    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;
    private static final long POLL_TIMEOUT_MS = 1_000L;

    private final EventBus bus;

    public EventsHandler(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().toUpperCase(Locale.ROOT).equals("GET")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        // length 0 ⇒ chunked response
        exchange.sendResponseHeaders(200, 0);

        BlockingQueue<HeraldEvent> queue = bus.subscribe();
        long lastHeartbeat = System.currentTimeMillis();
        try (OutputStream out = exchange.getResponseBody()) {
            // prime connection so the client unblocks early
            writeFrame(out, HeraldEvent.of("ready", emptyPayload()));

            while (!Thread.currentThread().isInterrupted()) {
                HeraldEvent event = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    writeFrame(out, event);
                }
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
                    writeFrame(out, HeraldEvent.of("heartbeat", emptyPayload()));
                    lastHeartbeat = now;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Client disconnected — normal for SSE.
            LOG.debug("SSE client disconnected: {}", e.getMessage());
        } finally {
            bus.unsubscribe(queue);
            exchange.close();
        }
    }

    private static JsonObject emptyPayload() {
        return new JsonObject();
    }

    private static void writeFrame(OutputStream out, HeraldEvent event) throws IOException {
        out.write(event.toSseFrame().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
