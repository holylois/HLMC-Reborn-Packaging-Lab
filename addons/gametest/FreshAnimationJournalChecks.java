package holylois;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import java.util.ArrayList;
import java.util.List;
import holylois.boombox.*;

/** Journal previews with the shipped Fresh Animations packs on: custom models, eyes and idle motion, without twitching poses. */
public final class FreshAnimationJournalChecks {
    private static final List<String> PACKS=List.of("file/FreshAnimations_v1.10.5.zip","file/FA+Objects-v2.1.2.zip","file/JustExpressions_v1.2.1.zip");
    public static void run(ClientGameTestContext context,TestSingleplayerContext world){
        var before=context.computeOnClient(client->new ArrayList<>(client.getResourcePackRepository().getSelectedIds()));
        select(context,withPacks(before));
        // GUI scale 1 gives the wide layout: list on the left, the 3D model in the detail pane.
        context.runOnClient(client->{client.options.guiScale().set(1);client.resizeGui();});
        for(String type:List.of("minecraft:zombie","minecraft:skeleton","minecraft:creeper","minecraft:spider","minecraft:cow","minecraft:chicken","minecraft:witch","minecraft:enderman")){
            var record=new DefeatRecord(type,10,20,false,false,1,"minecraft:overworld",0,64,0,List.of(),List.of(),List.of());
            var entry=new JournalState.Entry("kill:"+type,3,type,"","minecraft:overworld",0,64,0,1,1,0,0,10,true,false).withStrongest(record);
            context.runOnClient(client->{
                var screen=new JournalScreen(new JournalState(1,3,0,1,1,8,"",List.of(entry)));
                try{var paused=JournalScreen.class.getDeclaredField("previewPaused");paused.setAccessible(true);paused.setBoolean(screen,true);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                client.gui.setScreen(screen);
            });
            context.waitTicks(2);
            // Pin the page to this entry at the screen's real row count, so the fixture server reply cannot replace it.
            context.runOnClient(client->{
                var screen=(JournalScreen)client.gui.screen();
                try{var rows=JournalScreen.class.getDeclaredField("rows");rows.setAccessible(true);screen.update(new JournalState(1,3,0,1,1,rows.getInt(screen),"",List.of(entry)));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            });
            String name=type.substring(type.indexOf(':')+1);
            context.waitTicks(5);context.takeScreenshot("62-fa-"+name+"-a");
            context.waitTicks(17);context.takeScreenshot("62-fa-"+name+"-b");
            // Close-up: the same preview renderer filling the window, so eyes and small model parts can be judged.
            context.runOnClient(client->client.gui.setScreen(new CloseUp(record)));
            context.waitTicks(5);context.takeScreenshot("63-fa-close-"+name+"-a");
            context.waitTicks(30);context.takeScreenshot("63-fa-close-"+name+"-b");
            // Burst one tick apart: consecutive frames must move smoothly (no pose snapping or flicker).
            for(int f=0;f<8;f++){context.waitTicks(1);context.takeScreenshot("64-fa-burst-"+name+"-"+f);}
        }
        context.runOnClient(client->client.gui.setScreen(null));
        select(context,before);
    }
    private static final class CloseUp extends net.minecraft.client.gui.screens.Screen {
        private final JournalPreview preview=new JournalPreview();private final DefeatRecord record;
        CloseUp(DefeatRecord record){super(net.minecraft.network.chat.Component.empty());this.record=record;}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick){
            g.fill(0,0,width,height,0xFF14161A);preview.draw(g,record,width/2-height/2,10,height,height-20,false,0.5f);
        }
        @Override public boolean isPauseScreen(){return false;}
    }
    private static List<String> withPacks(List<String> selected){
        var ids=new ArrayList<>(selected);ids.removeAll(PACKS);ids.addAll(PACKS);return ids;
    }
    private static void select(ClientGameTestContext context,List<String> ids){
        var done=context.computeOnClient(client->{client.getResourcePackRepository().setSelected(ids);return client.reloadResourcePacks();});
        context.waitFor(client->done.isDone(),2400);context.waitTicks(160);
    }
}
