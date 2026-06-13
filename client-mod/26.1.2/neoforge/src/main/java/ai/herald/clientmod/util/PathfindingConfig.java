package ai.herald.clientmod.util;

/**
 * Tunable constants for movement / pathfinding actions.
 * Port of {@code RuntimeBlackBoxConfig.pathfinding} from the reference project
 * as plain {@code static final} fields — Herald-Client does not yet support
 * runtime reconfiguration.
 */
public final class PathfindingConfig {

    /** Maximum straight-line distance (blocks) a single move request may target. */
    public static final double MAX_DISTANCE = 64.0;

    /** Squared-block threshold below which the player is considered "arrived". */
    public static final double ARRIVAL_THRESHOLD = 0.8;

    /** Max ticks a move action is allowed to run before being aborted. */
    public static final int DEFAULT_TIMEOUT_TICKS = 200;

    /** Step height (blocks) for auto-jumping in navigate_to. */
    public static final double AUTO_JUMP_MIN_DY = 0.5;

    private PathfindingConfig() {}
}
