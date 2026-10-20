package holylois;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileQuarantineTest {
    private static int checked;
    public static void main(String[] args) throws Exception {
        var root = Files.createTempDirectory("quarantine-fixture");
        var source = root.resolve("playerdata/generated.dat"); Files.createDirectories(source.getParent()); Files.writeString(source, "generated fixture");
        var backup = root.resolve("holylois/backup-a");
        FileQuarantine.begin(root, backup, "generated-owner", List.of(source));
        check(!Files.exists(source) && Files.readString(backup.resolve("playerdata/generated.dat")).equals("generated fixture"), "Original bytes moved into recoverable backup");
        check(!FileQuarantine.resume(root, backup, "generated-owner"), "Committed relocation is idempotent");

        var resumed = root.resolve("holylois/backup-b"); Files.createDirectories(resumed.resolve("playerdata"));
        Files.writeString(resumed.resolve("playerdata/one.dat"), "one");
        var two = root.resolve("playerdata/two.dat"); Files.writeString(two, "two");
        var rows = List.of(item("playerdata/one.dat", "one"), item("playerdata/two.dat", "two"));
        journal(resumed, rows);
        Files.writeString(resumed.resolve("transaction.pending"), "interrupted completion write");
        check(FileQuarantine.resume(root, resumed, "generated-owner"), "Interrupted move resumes from source or verified destination");
        check(Files.readString(resumed.resolve("playerdata/two.dat")).equals("two") && !Files.exists(two), "Remaining member moved once");
        try (var entries = Files.list(resumed)) { check(entries.anyMatch(p -> p.getFileName().toString().startsWith("interrupted-")), "Interrupted journal retained"); }

        var conflict = root.resolve("holylois/conflict"); Files.createDirectories(conflict.resolve("playerdata"));
        var collision = root.resolve("playerdata/collision.dat"); Files.writeString(collision, "a");
        Files.writeString(conflict.resolve("playerdata/collision.dat"), "b"); journal(conflict, List.of(item("playerdata/collision.dat", "a")));
        blocked(() -> FileQuarantine.resume(root, conflict, "generated-owner"));
        check(Files.readString(collision).equals("a") && Files.readString(conflict.resolve("playerdata/collision.dat")).equals("b"), "Conflicting copies are never overwritten");
        blocked(() -> FileQuarantine.resume(root, conflict, "unrelated-owner"));
        var traversal = root.resolve("holylois/traversal"); Files.createDirectories(traversal);
        journal(traversal, List.of(item("../outside.dat", "x"))); blocked(() -> FileQuarantine.resume(root, traversal, "generated-owner"));
        var changed = root.resolve("holylois/changed"); Files.createDirectories(changed);
        journal(changed, List.of(item("playerdata/collision.dat", "different"))); blocked(() -> FileQuarantine.resume(root, changed, "generated-owner"));
        System.out.println("Passed " + checked + " recoverable relocation interruption/conflict/identity/path checks");
    }
    private static FileQuarantine.Item item(String path, String text) throws Exception {
        return new FileQuarantine.Item(path, text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
            java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
    private static void journal(Path folder, List<FileQuarantine.Item> rows) throws Exception {
        Files.writeString(folder.resolve("transaction.json"), new com.google.gson.Gson().toJson(new FileQuarantine.Plan(1, "generated-owner", rows, false)));
    }
    interface Checked { void run() throws Exception; }
    private static void blocked(Checked action) throws Exception { try { action.run(); throw new AssertionError("Unsafe relocation passed"); } catch (java.io.IOException expected) { checked++; } }
    private static void check(boolean ok, String message) { checked++; if (!ok) throw new AssertionError(message); }
}
