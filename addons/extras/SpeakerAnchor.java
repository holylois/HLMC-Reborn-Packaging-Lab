package holylois.boombox;

import net.minecraft.world.phys.Vec3;

/** A carried model can also render in a GUI. Such coordinates cannot anchor world particles. */
final class SpeakerAnchor {
    static boolean near(Vec3 holder, Vec3 left, Vec3 right) {
        return valid(holder,left) && valid(holder,right);
    }
    private static boolean valid(Vec3 holder, Vec3 point) {
        var delta=point.subtract(holder);
        return Double.isFinite(delta.x) && Double.isFinite(delta.y) && Double.isFinite(delta.z)
            && delta.horizontalDistanceSqr() <= 4 && delta.y >= -0.75 && delta.y <= 2.75;
    }
}
