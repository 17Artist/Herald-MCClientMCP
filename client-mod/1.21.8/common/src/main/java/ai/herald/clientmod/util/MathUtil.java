package ai.herald.clientmod.util;

/** Pure-math helpers shared by movement / look actions. */
public final class MathUtil {

    private MathUtil() {}

    public static double distance3D(double ax, double ay, double az,
                                    double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float yawTo(double dx, double dz) {
        return (float) (-Math.atan2(dx, dz) * 180.0 / Math.PI);
    }

    public static float pitchTo(double dx, double dy, double dz) {
        double dh = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.atan2(dy, dh) * 180.0 / Math.PI);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
