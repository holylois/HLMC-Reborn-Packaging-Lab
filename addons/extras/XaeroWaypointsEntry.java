package holylois.boombox;

/** Public door to XaeroWaypoints for the mixin package (the Xaero classes load only when this is called). */
public final class XaeroWaypointsEntry {
    private XaeroWaypointsEntry() {}

    public static void add(String arguments) { XaeroWaypoints.add(arguments); }
}
