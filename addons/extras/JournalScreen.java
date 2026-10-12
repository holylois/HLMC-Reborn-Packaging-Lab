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
    private boolean loading,detail,timedOut;private long requestedAt=System.currentTimeMillis();private int expectedRequest;
    private int listX,listY,listW,rows,detailX,detailW,detailScroll,maxScroll;
    private static final int ROW=30;
    public JournalScreen(int tab){super("journal_title");this.tab=tab;loading=true;}
    public JournalScreen(JournalState state){this(state.tab());update(state);}
    static Component label(String key,Object...args){return Component.translatable("holylois.journal."+key,args);}
    void expect(int id){expectedRequest=id;}
    public void update(JournalState next){
        if(next.requestId()!=0&&next.requestId()!=expectedRequest)return;
        if(next.requestId()==0)expectedRequest=0;
        state=next;tab=next.tab();filter=next.filter();page=next.page();query=next.search();loading=false;timedOut=false;
        if(next.entries().stream().noneMatch(e->e.key().equals(selected))){selected=next.entries().isEmpty()?"":next.entries().getFirst().key();detailScroll=0;}
        if(width>0)rebuildWidgets();
    }
    private boolean wide(){return panelWidth>=480;}
    private void fetch(){
        loading=true;timedOut=false;requestedAt=System.currentTimeMillis();detail=false;detailScroll=0;
        if(!JournalClient.request(new JournalRequest(tab,filter,page,rows,query))){loading=false;timedOut=true;}
        rebuildWidgets();
    }
    private void filter(int tab,int filter){if(this.tab==tab&&this.filter==filter)return;this.tab=tab;this.filter=filter;page=0;selected="";fetch();}
    private Button action(int x,int y,int w,Component text,Runnable run){return buttonText(x,y,w,text,run);}
    @Override protected void init(){
        search=null;
        panelWidth=Math.min(640,width-24);panelHeight=Math.min(400,height-24);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        rows=Math.max(1,Math.min(8,(panelHeight-126)/ROW));
        listX=left+12;listY=top+88;listW=wide()?(panelWidth-36)*3/5:panelWidth-24;detailX=listX+listW+12;detailW=panelWidth-24-listW-12;
        int content=panelWidth-24,half=(content-6)/2;
        if(!detail||wide()){
        action(listX,top+32,half,label("world"),()->filter(0,0)).active=!loading;
        action(listX+half+6,top+32,half,label("mine"),()->filter(1,0)).active=!loading;
        int sw=Math.max(50,content/3);
        search=addRenderableWidget(new EditBox(font,listX,top+62,sw,22,label("search")));search.setMaxLength(48);search.setValue(query);search.setHint(label("search"));
        action(listX+sw+6,top+62,44,label("find"),()->{query=search.getValue().strip();page=0;fetch();}).active=!loading;
        int[] filters=tab==0?new int[]{0,2,3}:new int[]{0,1,2,3};
        int fx=listX+sw+56,fw=Math.max(24,(content-sw-56)/filters.length);
        for(int i=0;i<filters.length;i++){int f=filters[i];var b=action(fx+i*fw,top+62,fw-3,label("filter"+f),()->filter(tab,f));b.active=!loading;if(f==4)b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label("your_first")));}
        }
        if(!detail||wide()){
            if(state!=null)for(int i=0;i<Math.min(rows,state.entries().size());i++){
                var e=state.entries().get(i);var b=new JournalRow(listX,listY+i*ROW,listW,ROW-4,e,()->{selected=e.key();detail=true;detailScroll=0;rebuildWidgets();});b.active=!loading;addRenderableWidget(b);
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
            if(tab==1){var firsts=action(listX+listW-(wide()?32:104),top+panelHeight-40,28,label("filter4"),()->filter(tab,4));firsts.active=!loading;firsts.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label("your_first")));}
        }
        action(left+panelWidth-72,top+panelHeight-40,60,label("close"),this::onClose);
        if(timedOut)action(listX+Math.max(0,(listW-80)/2),listY+56,80,label("retry"),this::fetch);
    }
    private JournalState.Entry selected(){return state==null?null:state.entries().stream().filter(e->e.key().equals(selected)).findFirst().orElse(null);}
    public static Component name(JournalState.Entry e){
        var id=Identifier.tryParse(e.name());
        if(id!=null&&e.kind()==JournalState.FISH)return capitalize(BuiltInRegistries.ITEM.getOptional(id).map(item->item.getName(new net.minecraft.world.item.ItemStack(item))).orElse(Component.literal(e.name())));
        if(id!=null&&e.kind()==JournalState.KILL)return capitalize(BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type->type.getDescription()).orElse(Component.literal(e.name())));
        String display=e.name().replaceFirst("^slew the first ","First ").replaceFirst("^slew ","Defeated ");
        if(!display.isEmpty())display=display.substring(0,1).toUpperCase(Locale.ROOT)+display.substring(1);
        return Component.literal(display);
    }
    private static Component capitalize(Component text){
        String value=text.getString();if(value.isEmpty())return text;
        int first=value.codePointAt(0),upper=Character.toUpperCase(first);
        return first==upper?text:Component.literal(new String(Character.toChars(upper))+value.substring(Character.charCount(first))).withStyle(text.getStyle());
    }
    private static Component title(JournalState.Entry entry){
        if(!entry.first())return name(entry).copy().withStyle(s->s.withColor(Ui.TEXT&0xFFFFFF));
        return name(entry).copy().withStyle(s->s.withColor(Ui.GOLD&0xFFFFFF));
    }
    private static net.minecraft.world.item.Item icon(JournalState.Entry entry){
        var id=Identifier.tryParse(entry.name());
        if(entry.kind()==JournalState.FISH&&id!=null)return BuiltInRegistries.ITEM.getOptional(id).orElse(net.minecraft.world.item.Items.COD);
        if(entry.kind()==JournalState.PLACE)return net.minecraft.world.item.Items.MAP;
        return switch(entry.name()){
            case "minecraft:zombie"->net.minecraft.world.item.Items.ZOMBIE_HEAD;
            case "minecraft:skeleton"->net.minecraft.world.item.Items.SKELETON_SKULL;
            case "minecraft:wither_skeleton"->net.minecraft.world.item.Items.WITHER_SKELETON_SKULL;
            case "minecraft:creeper"->net.minecraft.world.item.Items.CREEPER_HEAD;
            case "minecraft:piglin"->net.minecraft.world.item.Items.PIGLIN_HEAD;
            case "minecraft:ender_dragon"->net.minecraft.world.item.Items.DRAGON_HEAD;
            default->net.minecraft.world.item.Items.BOOK;
        };
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        int dx=wide()?detailX:listX,dw=wide()?detailW:listW;
        if(selected()!=null&&(wide()||detail)&&x>=dx&&x<=dx+dw&&y>=top+32&&y<top+panelHeight-76){
            detailScroll=Math.max(0,Math.min(maxScroll,detailScroll-(int)(vertical*22)));return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    static String mapName(String dim){return switch(dim){case "minecraft:overworld"->"world";case "minecraft:the_nether"->"world_the_nether";case "minecraft:the_end"->"world_the_end";default->null;};}
    private Component summary(JournalState.Entry e){
        Component text=e.kind()==JournalState.FEAT?label("first_by",e.by()):e.kind()==JournalState.FISH?label("caught",e.count()):e.kind()==JournalState.KILL?label(tab==0?"first_by":"defeated",tab==0?e.by():e.count())
            :tab==0?label("first_by",e.by()):label("visited");
        return e.first()&&(tab==0||e.kind()==JournalState.FEAT||e.kind()==JournalState.PLACE)?Component.literal("★ ").append(text).withStyle(s->s.withColor(Ui.GOLD&0xFFFFFF)):text;
    }
    @Override boolean enter(){if(search!=null&&search.isFocused()&&!loading){query=search.getValue().strip();page=0;fetch();return true;}return false;}
    @Override public void tick(){
        super.tick();
        if(loading&&System.currentTimeMillis()-requestedAt>5000){loading=false;timedOut=true;rebuildWidgets();}
        if(state!=null&&!loading&&!timedOut&&state.size()!=rows&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(JournalRequest.TYPE)){
            page=state.page()*state.size()/rows;fetch();
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        super.extractRenderState(g,mx,my,tick);g.nextStratum();
        if(!detail||wide()){
            int content=panelWidth-24,half=(content-6)/2,tx=tab==0?listX:listX+half+6;
            g.fill(tx+4,top+54,tx+half-4,top+55,Ui.GOLD);
            int sw=Math.max(50,content/3),fx=listX+sw+56,n=tab==0?3:4,fw=Math.max(24,(content-sw-56)/n);
            int selectedFilter=tab==0?(filter==3?2:filter==2?1:0):filter;
            if(filter!=4)g.fill(fx+selectedFilter*fw+3,top+83,fx+(selectedFilter+1)*fw-6,top+84,Ui.GOLD);
            else{int x=listX+listW-(wide()?32:104);g.fill(x+4,top+panelHeight-19,x+24,top+panelHeight-18,Ui.GOLD);}
        }
        if(timedOut)g.centeredText(font,label("timeout"),listX+listW/2,listY+24,Ui.TEXT);
        else if(state==null)g.centeredText(font,label("loading"),listX+listW/2,listY+24,Ui.MUTED);
        else if(loading)g.text(font,label("updating"),listX,top+22,Ui.MUTED,false);
        else if(state.entries().isEmpty()){
            String empty=query.isEmpty()?(tab==0?"empty_world":filter==0?"empty":"empty"+filter):"no_results";
            int y=listY+8;for(var line:font.split(label(empty),listW-12)){g.text(font,line,listX+6,y,Ui.MUTED,false);y+=12;}
        }
        if(state!=null&&(!detail||wide()))g.text(font,label("page",state.page()+1,state.pages(),state.total()),listX+74,top+panelHeight-33,Ui.MUTED,false);
        var entry=selected();
        if(!loading&&entry!=null&&(wide()||detail)){
            int dx=wide()?detailX:listX,dw=wide()?detailW:listW,dy=wide()?listY+6:top+38,max=top+panelHeight-76,boxY=dy-6;
            Ui.box(g,dx-3,boxY,dw+6,Math.max(20,max-boxY+6),Ui.RAISED,Ui.LINE);
            g.nextStratum();
            var lines=new ArrayList<Component>();
            lines.add(title(entry));
            if(tab==0||entry.kind()==JournalState.FEAT)lines.add(summary(entry));
            else{
                if(entry.first())lines.add(Component.literal("★ ").append(label("your_first")).withStyle(s->s.withColor(Ui.GOLD&0xFFFFFF)));
                lines.add(entry.kind()==JournalState.PLACE?label("visited"):summary(entry));
            }
            if(entry.kind()==JournalState.FISH){
                lines.add(label("best",entry.weight()>0?FishData.shortWeight(entry.weight())+" kg":label("unweighed").getString()));
                lines.addAll(rarityLines(entry));
                if(entry.shiny()>0)lines.add(label("shiny_count",entry.shiny()).copy().withStyle(s->s.withColor(Ui.GOLD&0xFFFFFF)));
            }
            if(entry.level()>0)lines.add(label("level",entry.level()));
            if(entry.time()>0){
                String date=DateTimeFormatter.ofPattern("d MMM yyyy",Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(entry.time()));
                lines.add(label(tab==0||entry.kind()==JournalState.FEAT?"first_on":entry.kind()==JournalState.FISH?"caught_on":entry.kind()==JournalState.KILL?"defeated_on":"visited_on",date));
            }
            if(entry.location()){
                lines.add(Component.literal(HomesScreen.dimension(entry.dimension())));
                lines.add(Component.literal(entry.x()+", "+entry.y()+", "+entry.z()));
            }else lines.add(label("no_location"));
            dy-=detailScroll;g.enableScissor(dx,boxY,dx+dw,max+2);
            for(var line:lines){for(var part:font.split(line,dw-8)){g.text(font,part,dx+4,dy,Ui.MUTED,false);dy+=11;}dy+=2;}
            g.disableScissor();maxScroll=Math.max(0,dy+detailScroll-max+10);
            if(maxScroll>0)g.text(font,Component.literal("↕"),dx+dw-8,boxY+2,Ui.MUTED,false);
        }
    }
    public static java.util.List<Component> rarityLines(JournalState.Entry entry){
        var lines=new ArrayList<Component>();
        String[] tiers={"common","uncommon","rare","epic","legendary","mythic"};
        int highest=31-Integer.numberOfLeadingZeros(entry.rarities()&63);
        for(int i=0;i<=highest;i++){
            long count=i<entry.rarityCounts().size()?entry.rarityCounts().get(i):0;
            int color=i==0?Ui.TEXT:FishData.color(tiers[i]);
            String tier=tiers[i];var rarity=label("rarity"+i).copy().withStyle(s->JournalHints.motionEnabled()?FishData.shine(s.withColor(color&0xFFFFFF),tier):s.withColor(color&0xFFFFFF));
            lines.add(rarity.append(Component.literal(": ")).append(count>0?Component.literal(Long.toString(count)):(entry.rarities()&(1<<i))!=0?label("old_count"):Component.literal("0")));
        }
        return lines;
    }
    private final class JournalRow extends Button {
        private final JournalState.Entry entry;
        JournalRow(int x,int y,int w,int h,JournalState.Entry entry,Runnable run){super(x,y,w,h,name(entry),b->run.run(),DEFAULT_NARRATION);this.entry=entry;if(font.width(name(entry))>w-36)setTooltip(net.minecraft.client.gui.components.Tooltip.create(name(entry)));}
        @Override protected void handleCursor(GuiGraphicsExtractor g){if(active)super.handleCursor(g);}
        @Override protected void extractContents(GuiGraphicsExtractor g,int x,int y,float tick){
            boolean activeRow=entry.key().equals(selected);
            Ui.box(g,getX(),getY(),getWidth(),getHeight(),activeRow?Ui.GOLD_SOFT:isHovered()?Ui.CONTROL_HOVER:Ui.CONTROL,activeRow?Ui.GOLD:Ui.LINE);
            g.nextStratum();
            g.item(new net.minecraft.world.item.ItemStack(icon(entry)),getX()+6,getY()+5);
            int color=entry.first()?Ui.GOLD:Ui.TEXT;
            var title=font.split(title(entry),getWidth()-36);if(!title.isEmpty())g.text(font,title.getFirst(),getX()+30,getY()+4,color,false);
            var description=font.split(summary(entry),getWidth()-36);if(!description.isEmpty())g.text(font,description.getFirst(),getX()+30,getY()+15,Ui.MUTED,false);
        }
    }
}
