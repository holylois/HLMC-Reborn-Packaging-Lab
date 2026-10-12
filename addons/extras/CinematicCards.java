package holylois.boombox;

import java.nio.file.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Personal override for cards over the cinematic camera. Catch/item card preferences still apply. */
public final class CinematicCards {
    private static boolean enabled=true;
    private static final Path FILE=Path.of("config","holylois-cinematic-cards.json");
    public static boolean enabled(){return enabled;}
    static void register(){
        try{if(Files.exists(FILE))enabled=Boolean.TRUE.equals(new com.google.gson.Gson().fromJson(Files.readString(FILE),Boolean.class));}catch(Exception ignored){}
        ClientCommandRegistrationCallback.EVENT.register((d,a)->d.register(literal("cinematiccards")
            .then(literal("on").executes(c->set(true))).then(literal("off").executes(c->set(false)))));
    }
    private static int set(boolean value){
        enabled=value;
        try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,Boolean.toString(value));}
        catch(Exception error){Minecraft.getInstance().player.sendSystemMessage(Component.translatable("holylois.cinematic.cards_save_error"));return 0;}
        Minecraft.getInstance().player.sendSystemMessage(Component.translatable(value?"holylois.cinematic.cards_on":"holylois.cinematic.cards_off"));return 1;
    }
}
