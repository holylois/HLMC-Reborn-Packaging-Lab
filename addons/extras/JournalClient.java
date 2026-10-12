package holylois.boombox;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.nio.file.Files;

/** The new control is seeded only once, on a free key. Existing bound or unbound choices stay personal. */
public final class JournalClient {
    static final String KEY="key.holylois.journal";
    private static int requestId;
    static void register(){
        JournalHints.register();
        ClientPlayNetworking.registerGlobalReceiver(JournalUpdate.TYPE,(notice,context)->JournalHints.added());
        boolean existing=true;
        try{
            var file=Minecraft.getInstance().gameDirectory.toPath().resolve("options.txt");
            existing=Files.exists(file)&&Files.readAllLines(file).stream().anyMatch(line->line.startsWith("key_"+KEY+":"));
        }catch(Exception ignored){/* Do not change a preference we cannot read. */}
        boolean seed=!existing;
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY,InputConstants.Type.KEYBOARD,InputConstants.UNKNOWN.getValue(),KeyMapping.Category.register(Identifier.fromNamespaceAndPath("holylois","journal"))));
        ClientLifecycleEvents.CLIENT_STARTED.register(client->{
            boolean repaired=repairBinding(key);
            var candidate=InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K);
            if(seed&&key.isUnbound()&&java.util.Arrays.stream(client.options.keyMappings).noneMatch(other->other!=key&&!other.getName().startsWith("key.debug.")&&other.matches(candidate))){
                key.setKey(candidate);KeyMapping.resetMapping();client.options.save();
            }
            else if(repaired){KeyMapping.resetMapping();client.options.save();}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.consumeClick())if(client.player!=null&&client.gui.screen()==null)open(1);});
        ClientPlayNetworking.registerGlobalReceiver(JournalState.TYPE,(state,context)->{
            if(context.client().gui.screen() instanceof JournalScreen screen)screen.update(state);
            else if(state.requestId()==0)context.client().gui.setScreen(new JournalScreen(state));
        });
    }
    /** Migrate only our invalid pre-SDL unbound value; preserve all valid player choices. */
    public static boolean repairBinding(KeyMapping key){
        if(!key.saveString().equals("key.keyboard.-1"))return false;
        key.setKey(InputConstants.UNKNOWN);
        return true;
    }
    static void open(int tab){
        if(!available())return;
        Minecraft.getInstance().gui.setScreen(new JournalScreen(tab));
        request(new JournalRequest(tab,0,0,6,""));
    }
    private static boolean available(){
        if(!ClientPlayNetworking.canSend(JournalRequest.TYPE)){
            var player=Minecraft.getInstance().player;
            if(player!=null)player.sendSystemMessage(Component.translatable("holylois.journal.unavailable"));return false;
        }
        return true;
    }
    static boolean request(JournalRequest request){
        if(!available()||!(Minecraft.getInstance().gui.screen() instanceof JournalScreen screen))return false;
        requestId=requestId==Integer.MAX_VALUE?1:requestId+1;
        screen.expect(requestId);
        ClientPlayNetworking.send(new JournalRequest(request.tab(),request.filter(),request.page(),request.size(),request.search(),requestId));return true;
    }
}
