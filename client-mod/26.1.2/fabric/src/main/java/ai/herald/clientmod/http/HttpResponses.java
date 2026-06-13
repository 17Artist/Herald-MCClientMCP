package ai.herald.clientmod.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Shared plumbing for writing JSON responses. */
public final class HttpResponses {

    public static final String CT_JSON = "application/json; charset=utf-8";

    private HttpResponses() {}

    public static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void writeJson(HttpExchange exchange, int status, com.google.gson.JsonElement body) throws IOException {
        writeJson(exchange, status, body.toString());
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
