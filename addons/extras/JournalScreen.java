package holylois.boombox;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Collection browser: tabs, filters and search beside a selected entry with explicit location actions. */
public final class JournalScreen extends TravelScreen {
    private JournalState state;private int tab,filter,page;private String query="",selected="";private EditBox search;
    private boolean loading,detail;
    private int listX,listY,listW,rows,detailX,detailW;
    private static final int ROW=30;
    public JournalScreen(int tab){super("journal_title");this.tab=tab;loading=true;}
    public JournalScreen(JournalState state){this(state.tab());update(state);}
    static Component label(String key,Object...args){return Component.translatable("holylois.journal."+key,args);}
    public void update(JournalState next){
        state=next;tab=next.tab();filter=next.filter();page=next.page();query=next.search();loading=false;
        if(next.entries().stream().noneMatch(e->e.key().equals(selected)))selected=next.entries().isEmpty()?"":next.entries().getFirst().key();
        if(width>0)rebuildWidgets();
    }
    private boolean wide(){return panelWidth>=480;}
    private void fetch(){
        loading=true;detail=false;
        if(!JournalClient.request(new JournalRequest(tab,filter,page,rows,query)))loading=false;
        rebuildWidgets();
    }
    private void filter(int tab,int filter){this.tab=tab;this.filter=filter;page=0;selected="";fetch();}
    private Button action(int x,int y,int w,Component text,Runnable run){return buttonText(x,y,w,text,run);}
    @Override protected void init(){
        search=null;
        panelWidth=Math.min(640,width-24);panelHeight=Math.min(400,height-24);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        rows=Math.max(1,Math.min(8,(panelHeight-126)/ROW));
        listX=left+12;listY=top+88;listW=wide()?(panelWidth-36)*3/5:panelWidth-24;detailX=listX+listW+12;detailW=panelWidth-24-listW-12;
        int content=panelWidth-24,half=(content-6)/2;
        if(!detail||wide()){
        action(listX,top+32,half,label("world"),()->filter(0,0)).active=!loading&&tab!=0;
        action(listX+half+6,top+32,half,label("mine"),()->filter(1,0)).active=!loading&&tab!=1;
        int sw=Math.max(50,content/3);
        search=addRenderableWidget(new EditBox(font,listX,top+62,sw,22,label("search")));search.setMaxLength(48);search.setValue(query);search.setHint(label("search"));
        action(listX+sw+6,top+62,44,label("find"),()->{query=search.getValue().strip();page=0;fetch();}).active=!loading;
        int[] filters=tab==0?new int[]{0,2,3}:new int[]{0,1,2,3};
        int fx=listX+sw+56,fw=Math.max(24,(content-sw-56)/filters.length);
        for(int i=0;i<filters.length;i++){int f=filters[i];action(fx+i*fw,top+62,fw-3,label("filter"+f),()->filter(tab,f)).active=!loading&&filter!=f;}
        }
        if(!detail||wide()){
            if(state!=null&&!loading)for(int i=0;i<Math.min(rows,state.entries().size());i++){
                var e=state.entries().get(i);var b=new JournalRow(listX,listY+i*ROW,listW,ROW-4,e,()->{selected=e.key();detail=true;rebuildWidgets();});addRenderableWidget(b);
            }
        }
        var entry=selected();
        if(entry!=null&&(!loading)&&(wide()||detail)){
            int dx=wide()?detailX:listX,dw=wide()?detailW:listW,dy=top+panelHeight-68;
            if(entry.location()){
                action(dx,dy,(dw-4)/2,label("waypoint"),()->{
                    if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap"))XaeroWaypoints.add(entry.dimension()+" "+entry.x()+" "+entry.y()+" "+entry.z()+" "+name(entry).getString());
                }).active=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap");
                action(dx+(dw+4)/2,dy,(dw-4)/2,label("map"),()->{
                    String map=mapName(entry.dimension());
                    if(map!=null)defaultHandleClickEvent(new ClickEvent.OpenUrl(URI.create("https://map.holylois.com/#"+map+":"+entry.x()+":"+entry.y()+":"+entry.z()+":120:0:0:0:1:flat")),minecraft,this);
                }).active=mapName(entry.dimension())!=null;
            }
            if(!wide())action(dx,top+panelHeight-40,70,label("back"),()->{detail=false;rebuildWidgets();});
        }
        if(!detail||wide()){
            action(listX,top+panelHeight-40,30,Component.literal("<"),()->{page--;fetch();}).active=!loading&&page>0;
            action(listX+36,top+panelHeight-40,30,Component.literal(">"),()->{page++;fetch();}).active=!loading&&state!=null&&page+1<state.pages();
        }
        action(left+panelWidth-72,top+panelHeight-40,60,label("close"),this::onClose);
    }
    private JournalState.Entry selected(){return state==null?null:state.entries().stream().filter(e->e.key().equals(selected)).findFirst().orElse(null);}
    public static Component name(JournalState.Entry e){
        var id=Identifier.tryParse(e.name());
        if(id!=null&&e.kind()==JournalState.FISH)return BuiltInRegistries.ITEM.getOptional(id).map(item->item.getName(new net.minecraft.world.item.ItemStack(item))).orElse(Component.literal(e.name()));
        if(id!=null&&e.kind()==JournalState.KILL)return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type->type.getDescription()).orElse(Component.literal(e.name()));
        return Component.literal(e.name());
    }
    static String mapName(String dim){return switch(dim){case "minecraft:overworld"->"world";case "minecraft:the_nether"->"world_the_nether";case "minecraft:the_end"->"world_the_end";default->null;};}
    private Component summary(JournalState.Entry e){
        return e.kind()==JournalState.FISH?label("caught",e.count()):e.kind()==JournalState.KILL?label(tab==0?"first_by":"defeated",tab==0?e.by():e.count())
            :tab==0?label("first_by",e.by()):label("visited");
    }
    @Override boolean enter(){if(search!=null&&search.isFocused()&&!loading){query=search.getValue().strip();page=0;fetch();return true;}return false;}
    @Override public void tick(){
        super.tick();
        if(state!=null&&!loading&&state.size()!=rows&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(JournalRequest.TYPE)){
            page=state.page()*state.size()/rows;fetch();
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        super.extractRenderState(g,mx,my,tick);g.nextStratum();
        if(!detail||wide()){
            int content=panelWidth-24,half=(content-6)/2,tx=tab==0?listX:listX+half+6;
            g.fill(tx+4,top+54,tx+half-4,top+55,Ui.GOLD);
        }
        if(loading||state==null)g.centeredText(font,label("loading"),listX+listW/2,listY+24,Ui.MUTED);
        else if(state.entries().isEmpty()){
            int y=listY+8;for(var line:font.split(label(query.isEmpty()?"empty":"no_results"),listW-12)){g.text(font,line,listX+6,y,Ui.MUTED,false);y+=12;}
        }
        if(state!=null&&(!detail||wide()))g.text(font,label("page",state.page()+1,state.pages(),state.total()),listX+74,top+panelHeight-33,Ui.MUTED,false);
        var entry=selected();
        if(!loading&&entry!=null&&(wide()||detail)){
            int dx=wide()?detailX:listX,dw=wide()?detailW:listW,dy=wide()?listY+6:top+38,max=top+panelHeight-76,boxY=dy-6;
            Ui.box(g,dx-3,boxY,dw+6,Math.max(20,max-boxY+6),Ui.RAISED,Ui.LINE);
            g.nextStratum();
            var lines=new ArrayList<Component>();
            lines.add(name(entry).copy().withStyle(s->s.withColor((entry.first()?Ui.GOLD:Ui.TEXT)&0xFFFFFF)));
            lines.add(summary(entry));
            if(entry.kind()==JournalState.FISH){
                lines.add(label("best",entry.weight()>0?FishData.shortWeight(entry.weight())+" kg":label("unweighed").getString()));
                var rarities=Component.empty();
                for(int i=0;i<6;i++)if((entry.rarities()&(1<<i))!=0){if(!rarities.getString().isEmpty())rarities.append(Component.literal(", "));rarities.append(label("rarity"+i));}
                lines.add(rarities);
            }
            if(entry.level()>0)lines.add(label("level",entry.level()));
            if(entry.time()>0&&wide())lines.add(Component.literal(DateTimeFormatter.ofPattern("d MMM yyyy",Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(entry.time()))));
            if(entry.location()){
                lines.add(Component.literal(HomesScreen.dimension(entry.dimension())));
                lines.add(Component.literal(entry.x()+", "+entry.y()+", "+entry.z()));
            }else lines.add(label("no_location"));
            for(var line:lines){for(var part:font.split(line,dw-8)){if(dy+9>max)break;g.text(font,part,dx+4,dy,Ui.MUTED,false);dy+=11;}dy+=2;}
        }
    }
    private final class JournalRow extends Button {
        private final JournalState.Entry entry;
        JournalRow(int x,int y,int w,int h,JournalState.Entry entry,Runnable run){super(x,y,w,h,name(entry),b->run.run(),DEFAULT_NARRATION);this.entry=entry;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int x,int y,float tick){
            boolean activeRow=entry.key().equals(selected);
            Ui.box(g,getX(),getY(),getWidth(),getHeight(),activeRow?Ui.GOLD_SOFT:isHovered()?Ui.CONTROL_HOVER:Ui.CONTROL,activeRow?Ui.GOLD:Ui.LINE);
            g.nextStratum();
            var id=Identifier.tryParse(entry.name());
            var item=entry.kind()==JournalState.FISH&&id!=null?BuiltInRegistries.ITEM.getOptional(id).orElse(net.minecraft.world.item.Items.COD)
                :entry.kind()==JournalState.PLACE?net.minecraft.world.item.Items.MAP:net.minecraft.world.item.Items.IRON_SWORD;
            g.item(new net.minecraft.world.item.ItemStack(item),getX()+6,getY()+5);
            int color=entry.first()?Ui.GOLD:Ui.TEXT;
            g.text(font,font.plainSubstrByWidth(name(entry).getString(),getWidth()-36),getX()+30,getY()+4,color,false);
            g.text(font,font.plainSubstrByWidth(summary(entry).getString(),getWidth()-36),getX()+30,getY()+15,Ui.MUTED,false);
        }
    }
}
