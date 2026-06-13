package ai.herald.clientmod.protocol;

/**
 * HTTP endpoint path constants. Matches {@code S2_CLIENT_MOD_TECH.md §3.1}.
 */
public final class HttpEndpoints {
    public static final String PING = "/ping";
    public static final String ACTIONS = "/actions";
    public static final String ACTION_PREFIX = "/action/";
    public static final String SKILL_PREFIX = "/skill/";
    public static final String SKILL_CANCEL_SUFFIX = "/cancel";
    public static final String EVENTS = "/events";
    public static final String SHUTDOWN = "/shutdown";

    private HttpEndpoints() {}
}
