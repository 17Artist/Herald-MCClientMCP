package ai.herald.clientmod;

/** Compile-time constants shared across the mod. */
public final class HeraldConstants {

    public static final String MOD_ID = "herald_client";
    public static final String MOD_NAME = "Herald Client";
    public static final String MOD_VERSION = "0.1.0";

    /** Primary Minecraft version this build targets. */
    public static final String MC_VERSION = "1.20.1";

    /** Token / port file directory under {@code <gameDir>/.herald/}. */
    public static final String STATE_DIR_NAME = ".herald";

    public static final String TOKEN_FILE_NAME = "client-token";
    public static final String PORT_FILE_NAME = "client-port";

    /** Inclusive port discovery range. */
    public static final int PORT_RANGE_START = 8888;
    public static final int PORT_RANGE_END = 8898;

    /** Synchronous HTTP -> action future timeout. */
    public static final long ACTION_TIMEOUT_MS = 30_000L;

    private HeraldConstants() {}
}
