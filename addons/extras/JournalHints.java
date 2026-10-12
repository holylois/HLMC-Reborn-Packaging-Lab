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
    private static boolean enabled=true,motion=true;private static long until,started,pendingUntil;private static int count;
    public static boolean motionEnabled(){return motion;}
    public static void added(){
        if(!enabled)return;long now=System.currentTimeMillis();
        if(now>=pendingUntil||started>0&&now>=until){count=0;started=0;}
        count++;pendingUntil=now+60_000;
        var client=Minecraft.getInstance();
        if(client.gui.screen()!=null||CinematicTweaks.playing()){started=0;until=0;}
        else if(started>0)until=now+5000;
    }
    static void register(){
        try{if(Files.exists(FILE)){
            var data=com.google.gson.JsonParser.parseString(Files.readString(FILE));
            if(data.isJsonPrimitive())enabled=data.getAsBoolean();
            else{var settings=data.getAsJsonObject();enabled=!settings.has("enabled")||settings.get("enabled").getAsBoolean();motion=!settings.has("motion")||settings.get("motion").getAsBoolean();}
        }}catch(Exception ignored){}
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{until=0;started=0;pendingUntil=0;count=0;});
        ClientCommandRegistrationCallback.EVENT.register((d,a)->d.register(literal("journalalerts")
            .then(literal("on").executes(c->setting(true))).then(literal("off").executes(c->setting(false)))
            .then(literal("motion").then(literal("on").executes(c->motion(true))).then(literal("off").executes(c->motion(false))))));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Identifier.fromNamespaceAndPath("holylois","journal_hint"),(g,t)->{
            var mc=Minecraft.getInstance();
            long now=System.currentTimeMillis();
            if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden()||CinematicTweaks.playing()||count==0||now>=pendingUntil)return;
            if(started==0){started=now;until=now+5000;}
            if(now>=until)return;long age=now-started;
            var title=JournalScreen.label(count>1?"updated_short":"updated",count);
            var key=KeyMapping.get(JournalClient.KEY);
            var action=key==null||key.isUnbound()?Component.literal("/journal"):key.getTranslatedKeyMessage();
            var hint=JournalScreen.label("open_hint",action);
            int available=g.guiWidth()/2-107,w=Math.max(mc.font.width(title),mc.font.width(hint));
            boolean belowChat=available>=96,book=belowChat&&available>=w+24;
            int x=belowChat?8:g.guiWidth()-w-8,y=belowChat?g.guiHeight()-29:12,offset=book?22:0;
            float alpha=Math.min(1,Math.min(age/160f,(until-now)/450f));
            float sweep=motion&&age>=250&&age<1050?(float)Math.sin(Math.PI*(age-250)/800)*0.35f:0;
            float scale=motion?1+0.04f*Math.max(0,1-age/180f):1;
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
            if(book)g.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK),0,2);
            outlined(g,mc,mc.font.plainSubstrByWidth(title.getString(),belowChat?available-offset:w),offset,0,Ui.alpha(Ui.mix(Ui.TEXT,Ui.GOLD,sweep),alpha));
            outlined(g,mc,mc.font.plainSubstrByWidth(hint.getString(),belowChat?available-offset:w),offset,11,Ui.alpha(Ui.MUTED,alpha));
            g.pose().popMatrix();
        });
    }
    private static void outlined(net.minecraft.client.gui.GuiGraphicsExtractor g,Minecraft mc,String text,int x,int y,int color){
        int stroke=(color&0xFF000000);g.text(mc.font,text,x-1,y,stroke,false);g.text(mc.font,text,x+1,y,stroke,false);
        g.text(mc.font,text,x,y-1,stroke,false);g.text(mc.font,text,x,y+1,stroke,false);g.text(mc.font,text,x,y,color,false);
    }
    private static int motion(boolean value){motion=value;return save("motion_saved");}
    private static int setting(boolean value){
        enabled=value;until=0;started=0;pendingUntil=0;count=0;
        return save(value?"alerts_on":"alerts_off");
    }
    private static int save(String message){
        try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,new com.google.gson.Gson().toJson(java.util.Map.of("enabled",enabled,"motion",motion)));}
        catch(Exception error){Minecraft.getInstance().player.sendSystemMessage(JournalScreen.label("alerts_save_error"));return 0;}
        Minecraft.getInstance().player.sendSystemMessage(JournalScreen.label(message));return 1;
    }
}
