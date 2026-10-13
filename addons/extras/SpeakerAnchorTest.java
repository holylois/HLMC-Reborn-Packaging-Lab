package holylois.boombox;

import net.minecraft.world.phys.Vec3;

public final class SpeakerAnchorTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var holder=new Vec3(1500,65,-900);
        check(SpeakerAnchor.near(holder, holder.add(-.6,.35,0), holder.add(-.2,.35,0)), "World speakers stay usable");
        check(!SpeakerAnchor.near(holder, holder.add(0,50,0), holder), "Sky anchor rejected");
        check(!SpeakerAnchor.near(holder, holder.add(150,0,0), holder), "GUI and stale far anchors rejected");
        check(!SpeakerAnchor.near(holder, new Vec3(Double.NaN,0,0), holder), "Non-finite anchor rejected");
        check(SpeakerAnchor.near(holder, holder.add(.5,1.8,0), holder.add(.3,1.8,0)), "Raised hand retained");
        System.out.println("Held-note world anchor checks passed.");
    }
}
