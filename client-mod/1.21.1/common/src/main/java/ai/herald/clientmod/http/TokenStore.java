package ai.herald.clientmod.http;

import ai.herald.clientmod.HeraldConstants;
import ai.herald.clientmod.util.HeraldLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Owns the secret token written to {@code <gameDir>/.herald/client-token}.
 *
 * <p>Contract ({@code S2_CLIENT_MOD_TECH.md §4.1}):
 * <ul>
 *   <li>Generate a random UUID on boot</li>
 *   <li>Persist the UUID string as a single line (trailing {@code \n}) UTF-8</li>
 *   <li>Delete the file on {@link #cleanup()} / JVM shutdown</li>
 * </ul>
 */
public final class TokenStore {

    private static final Logger LOG = HeraldLogger.of(TokenStore.class);

    private final String token;
    private final Path tokenFile;

    private TokenStore(String token, Path tokenFile) {
        this.token = token;
        this.tokenFile = tokenFile;
    }

    /** Write a fresh token to {@code dir/client-token} and return the handle. */
    public static TokenStore boot(Path dir) throws IOException {
        Files.createDirectories(dir);
        String token = UUID.randomUUID().toString();
        Path tokenFile = dir.resolve(HeraldConstants.TOKEN_FILE_NAME);
        Files.writeString(tokenFile, token + "\n", StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        LOG.info("Token written to {}", tokenFile);
        return new TokenStore(token, tokenFile);
    }

    public String token() {
        return token;
    }

    public Path tokenFile() {
        return tokenFile;
    }

    /** Idempotent — swallows IO errors (best-effort during shutdown). */
    public void cleanup() {
        try {
            Files.deleteIfExists(tokenFile);
        } catch (IOException e) {
            LOG.warn("Failed to delete token file {}: {}", tokenFile, e.getMessage());
        }
    }
}
