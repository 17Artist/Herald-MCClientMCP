package ai.herald.clientmod.stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP POST handler for receiving remote input events from the browser.
 * Accepts JSON body and forwards to the InputHandler.
 */
public final class StreamInputHandler implements HttpHandler {

    private final StreamServer.InputHandler inputHandler;

    public StreamInputHandler(StreamServer.InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS preflight
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (inputHandler != null) {
                inputHandler.onInput(body);
            }
        }

        exchange.sendResponseHeaders(204, -1);
    }
}
