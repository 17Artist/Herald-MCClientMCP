package ai.herald.clientmod.util;

import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HeraldException;
import net.minecraft.core.Direction;

/** Parse {@link Direction} from loose user strings. */
public final class DirectionUtil {

    private DirectionUtil() {}

    public static Direction fromString(String face) {
        if (face == null) {
            throw new HeraldException(ErrorCode.INVALID_PARAMS, "face must not be null");
        }
        switch (face.toLowerCase()) {
            case "down":
            case "bottom":
                return Direction.DOWN;
            case "up":
            case "top":
                return Direction.UP;
            case "north": return Direction.NORTH;
            case "south": return Direction.SOUTH;
            case "west":  return Direction.WEST;
            case "east":  return Direction.EAST;
            default:
                throw new HeraldException(ErrorCode.INVALID_PARAMS, "Unknown face: " + face);
        }
    }
}
