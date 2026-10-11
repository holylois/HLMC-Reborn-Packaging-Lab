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
final class JournalClient {
    static final String KEY="key.holylois.journal";
    static void register(){
        boolean existing=true;
        try{
            var file=Minecraft.getInstance().gameDirectory.toPath().resolve("options.txt");
            existing=Files.exists(file)&&Files.readAllLines(file).stream().anyMatch(line->line.startsWith("key_"+KEY+":"));
        }catch(Exception ignored){/* Do not change a preference we cannot read. */}
        boolean seed=!existing;
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY,InputConstants.Type.KEYBOARD,-1,KeyMapping.Category.register(Identifier.fromNamespaceAndPath("holylois","journal"))));
        ClientLifecycleEvents.CLIENT_STARTED.register(client->{
            var candidate=InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K);
            if(seed&&key.isUnbound()&&java.util.Arrays.stream(client.options.keyMappings).noneMatch(other->other!=key&&!other.getName().startsWith("key.debug.")&&other.matches(candidate))){
                key.setKey(candidate);KeyMapping.resetMapping();client.options.save();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.consumeClick())if(client.player!=null&&client.gui.screen()==null)open(1);});
        ClientPlayNetworking.registerGlobalReceiver(JournalState.TYPE,(state,context)->{
            if(context.client().gui.screen() instanceof JournalScreen screen)screen.update(state);
            else context.client().gui.setScreen(new JournalScreen(state));
        });
    }
    static void open(int tab){
        if(request(new JournalRequest(tab,0,0,6,"")))Minecraft.getInstance().gui.setScreen(new JournalScreen(tab));
    }
    static boolean request(JournalRequest request){
        if(!ClientPlayNetworking.canSend(JournalRequest.TYPE)){
            var player=Minecraft.getInstance().player;
            if(player!=null)player.sendSystemMessage(Component.translatable("holylois.journal.unavailable"));return false;
        }
        ClientPlayNetworking.send(request);return true;
    }
}
