package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import java.nio.file.*;

/** Quiet corner hint: batch first entries, no sound, no focus change. /journalalerts on|off is personal. */
public final class JournalHints {
    private static final Path FILE=Path.of("config","holylois-journal-alerts.json");
    private static boolean enabled=true;private static long until;private static int count;
    public static void added(){if(!enabled)return;long now=System.currentTimeMillis();count=now<until?count+1:1;until=now+5000;}
    static void register(){
        try{if(Files.exists(FILE))enabled=Boolean.TRUE.equals(new com.google.gson.Gson().fromJson(Files.readString(FILE),Boolean.class));}catch(Exception ignored){}
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{until=0;count=0;});
        ClientCommandRegistrationCallback.EVENT.register((d,a)->d.register(literal("journalalerts")
            .then(literal("on").executes(c->setting(true))).then(literal("off").executes(c->setting(false)))));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Identifier.fromNamespaceAndPath("holylois","journal_hint"),(g,t)->{
            var mc=Minecraft.getInstance();
            if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden()||CinematicTweaks.playing()||System.currentTimeMillis()>=until)return;
            var title=JournalScreen.label(count>1?"updated_many":"updated",count);
            var key=KeyMapping.get(JournalClient.KEY);
            var action=key==null||key.isUnbound()?Component.literal("/journal"):key.getTranslatedKeyMessage();
            var hint=JournalScreen.label("open_hint",action);
            int w=Math.min(g.guiWidth()-16,Math.max(mc.font.width(title),mc.font.width(hint))+20),x=g.guiWidth()-w-8,y=12;
            Ui.box(g,x,y,w,34,Ui.SURFACE,Ui.LINE);g.nextStratum();
            g.text(mc.font,mc.font.plainSubstrByWidth(title.getString(),w-16),x+8,y+6,Ui.TEXT,false);
            g.text(mc.font,mc.font.plainSubstrByWidth(hint.getString(),w-16),x+8,y+19,Ui.MUTED,false);
        });
    }
    private static int setting(boolean value){
        enabled=value;until=0;count=0;
        try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,Boolean.toString(value));}
        catch(Exception error){Minecraft.getInstance().player.sendSystemMessage(JournalScreen.label("alerts_save_error"));return 0;}
        Minecraft.getInstance().player.sendSystemMessage(JournalScreen.label(value?"alerts_on":"alerts_off"));return 1;
    }
}
