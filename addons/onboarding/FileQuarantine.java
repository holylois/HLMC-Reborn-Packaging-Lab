package holylois;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Durable roll-forward journal for reversible account-file relocation. No overwrites or deletion. */
final class FileQuarantine {
    record Item(String relative, long size, String sha256) {}
    record Plan(int schema, String owner, List<Item> files, boolean complete) {}
    private static final Gson JSON = new Gson();

    static void begin(Path world, Path backup, String owner, List<Path> sources) throws IOException {
        var items = new ArrayList<Item>();
        for (var source : sources) {
            safe(world, source);
            items.add(new Item(world.relativize(source).toString().replace('\\', '/'), Files.size(source), hash(source)));
        }
        if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Account backup already exists");
        Files.createDirectories(backup);
        try { Files.setPosixFilePermissions(backup, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")); }
        catch (UnsupportedOperationException ignored) { }
        safe(world, backup);
        write(backup, new Plan(1, owner, List.copyOf(items), false));
        resume(world, backup, owner);
    }

    static boolean resume(Path world, Path backup, String owner) throws IOException {
        safe(world, backup);
        var journal = backup.resolve("transaction.json"); safe(world, journal);
        Plan plan;
        try { plan = JSON.fromJson(Files.readString(journal), Plan.class); }
        catch (RuntimeException error) { throw new IOException("Invalid account relocation journal", error); }
        if (plan == null || plan.schema != 1 || !owner.equals(plan.owner) || plan.files == null || plan.files.isEmpty())
            throw new IOException("Account relocation identity mismatch");
        if (plan.complete) return false;
        var names = new java.util.HashSet<String>();
        for (var row : plan.files) {
            if (row.relative == null || !names.add(row.relative) || row.relative.contains("\\")
                    || row.size < 0 || row.sha256 == null || !row.sha256.matches("[a-f0-9]{64}"))
                throw new IOException("Invalid account relocation member");
            var source = world.resolve(row.relative); var target = backup.resolve(row.relative);
            safe(world, source); safe(backup, target);
            boolean from = Files.exists(source, LinkOption.NOFOLLOW_LINKS), to = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (from == to) throw new IOException("Ambiguous account relocation state; originals retained for staff review");
            verify(from ? source : target, row);
        }
        for (var row : plan.files) {
            var source = world.resolve(row.relative); var target = backup.resolve(row.relative);
            if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) continue; // Already moved before interruption.
            verify(source, row);
            Files.createDirectories(target.getParent()); safe(backup, target);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Account backup conflict");
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            verify(target, row);
            syncDirectory(source.getParent()); syncDirectory(target.getParent());
        }
        if (Files.exists(backup.resolve("transaction.pending"), LinkOption.NOFOLLOW_LINKS))
            Files.move(backup.resolve("transaction.pending"), backup.resolve("interrupted-" + java.util.UUID.randomUUID() + ".json"), StandardCopyOption.ATOMIC_MOVE);
        write(backup, new Plan(1, owner, plan.files, true));
        return true;
    }

    private static void verify(Path path, Item row) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) != row.size || !hash(path).equals(row.sha256))
            throw new IOException("Account file changed; relocation paused without overwriting either copy");
    }

    private static String hash(Path path) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                var block = new byte[65536]; for (int n; (n = in.read(block)) != -1;) digest.update(block, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    static void safe(Path root, Path target) throws IOException {
        root = root.toAbsolutePath().normalize(); target = target.toAbsolutePath();
        if (!target.normalize().equals(target) || !target.startsWith(root)) throw new IOException("Account path escapes its scope");
        for (var p = target; p != null && p.startsWith(root); p = p.getParent())
            if (Files.isSymbolicLink(p)) throw new IOException("Linked account path blocked");
    }

    private static void write(Path backup, Plan plan) throws IOException {
        var next = backup.resolve("transaction.pending");
        try (var channel = FileChannel.open(next, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var bytes = java.nio.ByteBuffer.wrap(JSON.toJson(plan).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
        Files.move(next, backup.resolve("transaction.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        syncDirectory(backup);
    }

    private static void syncDirectory(Path path) throws IOException {
        if (System.getProperty("os.name").startsWith("Windows")) return; // Production is Linux; NTFS rejects directory handles.
        try (var channel = FileChannel.open(path, StandardOpenOption.READ)) { channel.force(true); }
    }
}
