package ai.herald.clientmod.util;

import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HeraldException;
import net.minecraft.world.InteractionHand;

/** Parse {@link InteractionHand} from loose user strings. */
public final class HandUtil {

    private HandUtil() {}

    public static InteractionHand fromString(String hand) {
        if (hand == null) return InteractionHand.MAIN_HAND;
        switch (hand.toLowerCase()) {
            case "main_hand":
            case "mainhand":
            case "main":
                return InteractionHand.MAIN_HAND;
            case "off_hand":
            case "offhand":
            case "off":
                return InteractionHand.OFF_HAND;
            default:
                throw new HeraldException(ErrorCode.INVALID_PARAMS, "Unknown hand: " + hand);
        }
    }
}
