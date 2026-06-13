package ai.herald.clientmod.http;

import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.ResponseMessage;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates every inbound request against the boot-time token.
 *
 * <p>Accepts either {@code Authorization: Bearer <t>} (preferred) or a
 * {@code ?token=<t>} query parameter (fallback). Bearer wins when both
 * are present. On rejection, writes a standard error envelope and 401.
 */
public final class TokenFilter extends Filter {

    private final String expectedToken;

    public TokenFilter(String expectedToken) {
        if (expectedToken == null || expectedToken.isEmpty()) {
            throw new IllegalArgumentException("expectedToken must not be empty");
        }
        this.expectedToken = expectedToken;
    }

    @Override
    public String description() {
        return "HeraldTokenFilter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String presented = extractToken(exchange);
        if (presented == null || !expectedToken.equals(presented)) {
            reject(exchange);
            return;
        }
        chain.doFilter(exchange);
    }

    static String extractToken(HttpExchange exchange) {
        // 1. Authorization: Bearer <t>
        List<String> auth = exchange.getRequestHeaders().get("Authorization");
        if (auth != null) {
            for (String v : auth) {
                if (v != null && v.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    String t = v.substring(7).trim();
                    if (!t.isEmpty()) return t;
                }
            }
        }
        // 2. ?token=<t>
        URI uri = exchange.getRequestURI();
        String q = uri.getRawQuery();
        if (q != null) {
            for (String pair : q.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private static void reject(HttpExchange exchange) throws IOException {
        ResponseMessage msg = ResponseMessage.error(ErrorCode.TOKEN_INVALID, "Invalid or missing token");
        byte[] body = msg.toJson().toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(msg.httpStatus(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
