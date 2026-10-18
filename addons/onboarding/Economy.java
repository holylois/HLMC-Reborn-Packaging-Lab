package holylois;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

/** EconomyCraft bridge: daily coin state, plus balance changes for Holy Lois features (land claims). */
final class Economy {
    /** Use EconomyCraft's own daily operation and ledger; never duplicate a raw payment. */
    static long claimDaily(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        long amount = unclaimedDaily(server, player.getUUID());
        if (amount <= 0) return 0;
        try { return Bridge.manager(server).claimDaily(player.getUUID()) ? amount : 0; }
        catch (RuntimeException | LinkageError error) { org.slf4j.LoggerFactory.getLogger("HolyLois").warn("Daily coin claim failed; manual /daily remains available", error); return 0; }
    }
    private Economy() {}

    private static boolean present() { return FabricLoader.getInstance().isModLoaded("economycraft"); }

    /** Coins /daily would pay now, or -1 when they were already claimed today or EconomyCraft is missing. */
    static long unclaimedDaily(MinecraftServer server, UUID id) {
        if (!present()) return -1;
        try { return Bridge.unclaimedDaily(server, id); } catch (LinkageError error) { broken(error); return -1; }
    }

    static boolean available() { return present(); }

    static long balance(MinecraftServer server, UUID id) {
        if (!present()) return 0;
        try { return Bridge.balance(server, id); } catch (LinkageError error) { broken(error); return 0; }
    }

    /** Takes coins only if the player has enough; false means nothing changed. */
    static boolean withdraw(MinecraftServer server, UUID id, long amount) {
        if (!present() || amount < 0) return false;
        try { return Bridge.withdraw(server, id, amount); } catch (LinkageError error) { broken(error); return false; }
    }

    static void deposit(MinecraftServer server, UUID id, long amount) {
        if (!present() || amount <= 0) return;
        try { Bridge.deposit(server, id, amount); } catch (LinkageError error) { broken(error); }
    }

    // An EconomyCraft update that changes its API (1.11 did, and crashed the server on join) must cost the coin features,
    // never the server tick.
    private static boolean warned;
    private static void broken(LinkageError error) {
        if (warned) return;
        warned = true;
        org.slf4j.LoggerFactory.getLogger("HolyLois").error("EconomyCraft API changed; Holy Lois coin features are off until the bridge is updated", error);
    }

    // Separate class so EconomyCraft types load only when the mod is present.
    private static final class Bridge {
        static com.reazip.economycraft.EconomyManager manager(MinecraftServer server) { return com.reazip.economycraft.EconomyCraft.getManager(server); }
        static long unclaimedDaily(MinecraftServer server, UUID id) {
            var manager = manager(server);
            if (manager == null || manager.hasClaimedDailyToday(id)) return -1;
            var config = com.reazip.economycraft.EconomyConfig.get();
            return config == null || config.economy == null ? -1 : config.economy.dailyReward;
        }
        private static final com.reazip.economycraft.api.v1.MutationSource SOURCE = com.reazip.economycraft.api.v1.MutationSource.of("holylois:server");
        static long balance(MinecraftServer server, UUID id) {
            var manager = manager(server);
            Long value = manager == null ? null : manager.getBalance(id, false);
            return value == null ? 0 : value;
        }
        static boolean withdraw(MinecraftServer server, UUID id, long amount) {
            var manager = manager(server);
            return manager != null && balance(server, id) >= amount && manager.removeMoney(id, amount, SOURCE).successful();
        }
        static void deposit(MinecraftServer server, UUID id, long amount) {
            var manager = manager(server);
            if (manager != null) manager.addMoney(id, amount, SOURCE);
        }
    }
}
