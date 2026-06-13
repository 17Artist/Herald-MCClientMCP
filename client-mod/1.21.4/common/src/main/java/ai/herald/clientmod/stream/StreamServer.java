package ai.herald.clientmod.stream;

import ai.herald.clientmod.util.HeraldLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;

import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MJPEG stream server. Browsers connect via <img src="/stream"> and receive
 * a continuous multipart/x-mixed-replace stream of JPEG frames.
 * This avoids WebSocket complexity with JDK HttpServer.
 */
public final class StreamServer implements HttpHandler {

    private static final Logger LOG = HeraldLogger.of(StreamServer.class);
    private static final String BOUNDARY = "herald-frame-boundary";

    private final CopyOnWriteArrayList<OutputStream> clients = new CopyOnWriteArrayList<>();
    private final InputHandler inputHandler;

    public StreamServer(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public boolean hasClients() {
        return !clients.isEmpty();
    }

    public int clientCount() {
        return clients.size();
    }

    public InputHandler inputHandler() {
        return inputHandler;
    }

    /**
     * Push a JPEG frame to all connected MJPEG clients.
     */
    public void broadcast(byte[] jpegData) {
        if (clients.isEmpty()) return;
        byte[] header = ("--" + BOUNDARY + "\r\n"
            + "Content-Type: image/jpeg\r\n"
            + "Content-Length: " + jpegData.length + "\r\n"
            + "\r\n").getBytes();

        for (OutputStream out : clients) {
            try {
                out.write(header);
                out.write(jpegData);
                out.write("\r\n".getBytes());
                out.flush();
            } catch (IOException e) {
                clients.remove(out);
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set MJPEG streaming headers
        exchange.getResponseHeaders().set("Content-Type",
            "multipart/x-mixed-replace; boundary=" + BOUNDARY);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0); // chunked

        OutputStream out = exchange.getResponseBody();
        clients.add(out);
        LOG.info("MJPEG client connected (total: {})", clients.size());

        // Keep the connection alive until client disconnects
        try {
            // Block this handler thread — the connection stays open
            // until the client disconnects (write fails in broadcast)
            while (clients.contains(out)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Shutting down
        } finally {
            clients.remove(out);
            try { out.close(); } catch (IOException ignored) {}
            LOG.info("MJPEG client disconnected (total: {})", clients.size());
        }
    }

    @FunctionalInterface
    public interface InputHandler {
        void onInput(String jsonMessage);
    }
}
