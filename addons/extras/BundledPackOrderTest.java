package holylois.boombox;

import java.util.*;
import java.util.function.Function;

public final class BundledPackOrderTest {
    static List<String> arrange(List<String> values) { return BundledPackOrder.arrange(values, Function.identity(), id -> id.equals("vanilla") || id.startsWith("mod:")); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var shuffled = new ArrayList<String>(BundledPackOrder.IDS); Collections.reverse(shuffled);
        shuffled.add(0,"file/PlayerA.zip"); shuffled.add(2,"mod:fishofthieves"); shuffled.add("vanilla");
        shuffled.add("file/PlayerB.zip"); shuffled.add("server/live");
        var fixed = arrange(shuffled);
        check(fixed.subList(0,2).equals(List.of("mod:fishofthieves","vanilla")), "Base resources keep their relative order");
        check(fixed.subList(2,9).equals(BundledPackOrder.IDS), "Managed packs regain their exact order above mod assets");
        check(fixed.subList(9,12).equals(List.of("file/PlayerA.zip","file/PlayerB.zip","server/live")), "Personal/server overlays keep relative order");
        check(arrange(fixed)==fixed, "Correct order needs no rebuild");
        var personalOnly=List.of("file/User.zip","vanilla");
        check(arrange(personalOnly)==personalOnly, "Unrelated repositories are untouched");
        check(arrange(List.of(BundledPackOrder.IDS.getLast(),"vanilla")).equals(List.of("vanilla",BundledPackOrder.IDS.getLast())), "Missing optional files do not fabricate entries");
        System.out.println("Managed pack priority and independent overlays passed.");
    }
}
