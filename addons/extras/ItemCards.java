package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Item cards (owner 2026-10-11): picking up something notable shows a card like the rare catch card. Holy Lois items, loot
 * crates, enchanted tools/weapons/armor and books, and items the game marks Rare or Epic. Client only (ItemCardPickupMixin
 * sees the pickup). At most one card every 4 s, three waiting, and the same item only once per 30 s, so dropping and picking
 * something up again stays quiet. Trophy fish keep their own catch card. /itemcards on|off.
 */
final class ItemCards {
    private ItemCards() {}

    record Card(ItemStack stack, String rarity, String label, String sub) {}

    private static final Path FILE = Path.of("config", "holylois-item-cards.json");
    private static final long GAP = 4000, SAME_ITEM = 30_000, LIFE = 4000;
    private static final ArrayDeque<Card> waiting = new ArrayDeque<>();
    private static final Map<String, Long> lastSeen = new HashMap<>();
    private static Card current;
    private static long shown, lastShown;
    private static boolean enabled = true;
    /** Cards shown (gametest). */
    static int cardsShown;

    static void register() {
        CrateStyle.register();
        try { if (Files.exists(FILE)) enabled = new com.google.gson.Gson().fromJson(Files.readString(FILE), Boolean.class); } catch (Exception ignored) {}
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((d, a) -> d.register(
            net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("itemcards")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("on").executes(c -> setting(true)))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("off").executes(c -> setting(false)))));
    }

    static void clear() { waiting.clear(); current = null; }

    /** From ItemCardPickupMixin: the local player picked up this stack. */
    static void pickedUp(ItemStack stack) {
        if (!enabled || stack.isEmpty()) return;
        Card card = cardFor(stack);
        if (card == null) return;
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        long now = System.currentTimeMillis();
        Long before = lastSeen.put(key, now);
        if (before != null && now - before < SAME_ITEM) return;
        if (waiting.size() < 3) waiting.add(card);
    }

    /** The card for a stack, or null when it is not notable. */
    static Card cardFor(ItemStack stack) {
        if (FishData.weighed(stack) || FishData.fillet(stack)) return null;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = stack.getHoverName().getString();
        int count = stack.getCount();
        String amount = count > 1 ? count + "x " : "";
        if (id.getNamespace().equals("holylois")) return new Card(stack, "legendary", label("holylois"), amount + name);
        String crate=CrateStyle.rarity(id.toString());
        if (!crate.isEmpty()) {
            int tier=switch(crate){case "uncommon"->1;case "rare"->2;default->4;};
            return new Card(stack,crate,label("crate"),Component.translatable("holylois.journal.rarity"+tier).getString()+(count>1?" - "+count+"x":""));
        }
        var enchants = stack.getEnchantments();
        var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        int levels = 0, kinds = 0;
        String best = null; int bestLevel = 0;
        for (var source : new net.minecraft.world.item.enchantment.ItemEnchantments[] {enchants, stored}) {
            if (source == null) continue;
            for (var entry : source.entrySet()) {
                kinds++; levels += entry.getIntValue();
                if (entry.getIntValue() > bestLevel || best == null) {
                    bestLevel = entry.getIntValue();
                    best = net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString();
                }
            }
        }
        boolean gear = stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON) || stack.has(DataComponents.EQUIPPABLE);
        if (kinds > 0 && (gear || stored != null)) {
            String more = kinds > 1 ? " +" + (kinds - 1) : "";
            return new Card(stack, kinds >= 4 || levels >= 12 ? "epic" : "rare", label("enchanted"), best + more);
        }
        String tier = HolyRarity.of(stack);
        if (HolyRarity.rank(tier) >= HolyRarity.rank("legendary")) return new Card(stack, "legendary", label("legendary"), amount + name);
        if (tier.equals("epic")) return new Card(stack, "epic", label("epic"), amount + name);
        if (tier.equals("rare")) return new Card(stack, "rare", label("rare"), amount + name);
        return null;
    }

    private static String label(String kind) { return Component.translatable("holylois.card." + kind).getString().toUpperCase(Locale.ROOT); }

    /** Drawn by RareCatchHud.card when no fish card is up (also during the cinematic). */
    static void card(net.minecraft.client.gui.GuiGraphicsExtractor g, Minecraft mc, boolean cinematic) {
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (current == null) {
            if (waiting.isEmpty() || now - lastShown < GAP) return;
            current = waiting.poll(); shown = 0;
        }
        if (!cinematic && mc.gui.hud.isHidden() || mc.gui.screen() != null) return;
        if (shown == 0) {
            shown = now; lastShown = now; cardsShown++;
            mc.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, current.rarity().equals("legendary") ? 1.5f : 1.2f);
        }
        long age = now - shown;
        if (age > LIFE) { current = null; lastShown = now; return; }
        // Shorter than a catch card: the same slide, shine and fade played over 4 s.
        RareCatchHud.draw(g, mc, current.stack(), age * RareCatchHud.LIFE / LIFE, current.rarity(), current.label(), current.sub(), false);
    }

    private static int setting(boolean value) {
        var p = Minecraft.getInstance().player;
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, new com.google.gson.Gson().toJson(value)); enabled = value; clear(); }
        catch (java.io.IOException error) { if (p != null) p.sendSystemMessage(Component.translatable("holylois.party.save_failed")); return 0; }
        if (p != null) p.sendSystemMessage(Component.translatable("holylois.card." + (value ? "enabled" : "disabled")));
        return 1;
    }
}
