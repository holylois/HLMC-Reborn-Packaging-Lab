package holylois.boombox;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** The managed block stays above mod resources and below player/server overlays on every selection rebuild. */
public final class BundledPackOrder {
    public static final List<String> IDS = List.of(
        "file/EatingAnimation-1.0.1.zip", "file/EatingAnimation-FarmersDelight-1.20-26.x.zip",
        "file/HolyLois-FishEating.zip", "file/FreshAnimations_v1.10.5.zip", "file/FA+Objects-v2.1.2.zip",
        "file/JustExpressions_v1.2.1.zip", "file/FA+Player-v1.1.zip");

    public static <T> List<T> arrange(List<T> selected, Function<T,String> identifier, Predicate<T> base) {
        var managed = new ArrayList<T>(); var lower = new ArrayList<T>(); var overlays = new ArrayList<T>();
        for (var pack : selected) {
            if (IDS.contains(identifier.apply(pack))) managed.add(pack);
            else if (base.test(pack)) lower.add(pack);
            else overlays.add(pack);
        }
        if (managed.isEmpty()) return selected;
        managed.sort(Comparator.comparingInt(pack -> IDS.indexOf(identifier.apply(pack))));
        var result = new ArrayList<T>(selected.size());
        result.addAll(lower); result.addAll(managed); result.addAll(overlays);
        return result.equals(selected) ? selected : List.copyOf(result);
    }
}
