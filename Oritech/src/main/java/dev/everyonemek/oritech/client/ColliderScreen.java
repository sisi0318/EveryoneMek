package dev.everyonemek.oritech.client;

import java.util.*;
import dev.everyonemek.oritech.Content;
import dev.everyonemek.oritech.AddonReadout;
import dev.everyonemek.oritech.collider.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.api.screen.*;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.init.BlockContent;

/** Native Oritech panels surround a server-authoritative virtual beamline editor. */
public final class ColliderScreen extends OritechWidgetScreen<ColliderMenu> {
    private final List<UIComponent> pageComponents=new ArrayList<>();
    private final List<ButtonWidget> editButtons=new ArrayList<>();
    private final List<ButtonWidget> sideButtons=new ArrayList<>();
    private ButtonWidget run,cancel,eject,automatic,lock,rotate,bendButton,lineMaterial,guideMaterial;
    private LabelWidget state,speed,recipeName,smartHint,magnetStats;
    private EditBox search;
    private ScrollWidget recipeList;
    private Board board;
    private int page,tool,direction,bend;
    private boolean smart=true;
    private int manualSource=Collider.PART_IN;
    private String query="";
    private double panX=23,panY=27,cellSize=12;
    private static final int BOARD_X=8,BOARD_Y=56,BOARD_W=216,BOARD_H=80;
    private static final String[] DIRECTIONS={"→","↘","↓","↙","←","↖","↑","↗"};
    public ColliderScreen(ColliderMenu menu,Inventory inventory,Component title){super(menu,inventory,title,320,240);}
    @Override public boolean shouldCreateTitle(){return false;}
    public static net.minecraft.network.chat.MutableComponent tr(String key,Object...args){return Component.translatable("collider.oritechmekanism."+key,args);}
    @Override public BlockState getTitleState(){return Content.COLLIDER.get().defaultBlockState();}
    @Override protected void buildComponents(){
        pageComponents.clear();editButtons.clear();sideButtons.clear();
        addComponent(new SurfaceWidget(0,0,320,240,OritechSurface.PANEL).withZIndex(-10));
        addComponent(new LabelWidget(8,6,304,10,title).withDarkColor());
        int tabs=MagnetSupport.loaded()?4:3;for(int i=0;i<tabs;i++){int tab=i;addComponent(ButtonWidget.panel(8+i*(tabs==4?55:74),20,tabs==4?51:68,14,tr(new String[]{"track","recipes","ports","magnetic"}[i]),b->send(11,tab,0,0,"")));}
        addComponent(new SurfaceWidget(232,28,80,126,OritechSurface.PANEL_DARK));
        addComponent(new LabelWidget(237,31,34,9,tr("input_a")));addComponent(new LabelWidget(279,31,32,9,tr("input_b")));
        addComponent(new ItemSlotWidget(ColliderMenu.INPUT_A_X,ColliderMenu.INPUT_Y));addComponent(new ItemSlotWidget(ColliderMenu.INPUT_B_X,ColliderMenu.INPUT_Y));
        addExpectedInput(0,ColliderMenu.INPUT_A_X,ColliderMenu.INPUT_Y);addExpectedInput(1,ColliderMenu.INPUT_B_X,ColliderMenu.INPUT_Y);
        addComponent(new LabelWidget(245,74,58,9,tr("output")).withAlignment(LabelWidget.Alignment.CENTER));addComponent(new ItemSlotWidget(ColliderMenu.OUTPUT_X,ColliderMenu.OUTPUT_Y));
        recipeName=new LabelWidget(236,64,72,9,Component.empty()).withAlignment(LabelWidget.Alignment.CENTER);addComponent(recipeName);
        addComponent(new UIComponent(236,110,72,7){@Override protected void renderContent(GuiGraphics g,int mx,int my,float delta){
            long amount=menu.view.getLong("energy");g.fill(x,y,x+width,y+height,0xff22282d);g.fill(x,y,x+(int)(width*Math.clamp((double)amount/Collider.CAPACITY,0,1)),y+height,0xff5aac87);
            withTooltip(Component.literal(String.format(Locale.ROOT,"%,d / %,d FE",amount,Collider.CAPACITY)));
            if(menu.view.getCompound("magnet").getLong("capacity")>0)addTooltipLine(tr("magnet_energy",String.format(Locale.ROOT,"%,d FE",menu.view.getLong("magneticEnergy"))));}});
        speed=new LabelWidget(236,120,72,10,Component.empty());addComponent(speed);
        state=new LabelWidget(236,132,72,18,Component.empty()).withWrap(true);addComponent(state);
        run=ButtonWidget.panel(252,162,58,16,tr("start"),b->send(0,0,0,0,""));addComponent(run);
        cancel=ButtonWidget.panel(252,184,58,16,tr("cancel"),b->send(1,0,0,0,""));cancel.withTooltip(tr("cancel_hint"));addComponent(cancel);
        automatic=ButtonWidget.panel(8,195,66,16,tr("auto"),b->send(9,0,0,0,""));addComponent(automatic);
        lock=ButtonWidget.panel(8,215,66,16,tr("lock_current"),b->send(10,0,0,0,""));addComponent(lock);
        eject=ButtonWidget.panel(252,211,58,16,tr("eject_on"),b->send(2,0,0,0,""));addComponent(eject);
        lineMaterial=ButtonWidget.panel(ColliderMenu.STRAIGHT_X,150,22,12,tr("stock_line"),b->{manualSource=Collider.STRAIGHT_PARTS;smart=false;tool=0;});lineMaterial.withTooltip(tr("stock_line_hint"));addComponent(lineMaterial);
        guideMaterial=ButtonWidget.panel(ColliderMenu.GUIDE_X,150,22,12,tr("stock_guide"),b->{manualSource=Collider.PART_IN;smart=false;tool=0;});guideMaterial.withTooltip(tr("stock_guide_hint"));addComponent(guideMaterial);
        supplySlot(ColliderMenu.STRAIGHT_X,Collider.STRAIGHT_PARTS,"stock_line_hint");supplySlot(ColliderMenu.GUIDE_X,Collider.PART_IN,"stock_guide_hint");
        addComponent(new LabelWidget(ColliderMenu.RETURN_X,151,22,10,tr("stock_return")).withDarkColor());addComponent(new ItemSlotWidget(ColliderMenu.RETURN_X,ColliderMenu.PART_Y));
        addComponent(new LabelWidget(ColliderMenu.INVENTORY_X,143,162,10,Component.translatable("container.inventory")).withDarkColor());
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addComponent(new ItemSlotWidget(ColliderMenu.INVENTORY_X+x*18,ColliderMenu.INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addComponent(new ItemSlotWidget(ColliderMenu.INVENTORY_X+x*18,ColliderMenu.INVENTORY_Y+58));
        search=addRenderableWidget(new EditBox(font,leftPos+8,topPos+37,216,16,tr("search")));search.setMaxLength(80);search.setHint(tr("search"));search.setValue(query);
        search.setResponder(value->{query=value;refreshRecipes();});buildPage();
    }
    private void supplySlot(int x,int slot,String hint){addComponent(new ItemSlotWidget(x,ColliderMenu.PART_Y){@Override public void tick(){setTooltip(menu.collider.inventory.getItem(slot).isEmpty()?List.of(tr(hint)):null);}});}
    private void page(UIComponent component){pageComponents.add(component);addComponent(component);}
    private void buildPage(){for(var c:pageComponents)removeComponent(c);pageComponents.clear();editButtons.clear();sideButtons.clear();recipeList=null;board=null;smartHint=null;magnetStats=null;
        search.setVisible(page==1);search.setFocused(false);
        if(page==0){
            String[] keys={"place","remove","move_a","move_b"};int[] widths={32,34,36,36};int x=8;
            for(int i=0;i<4;i++){final int t=i;var button=ButtonWidget.panel(x,37,widths[i],16,tr(keys[i]),b->{if(t==0&&tool==0)smart=!smart;tool=t;});button.withTooltip(tr(i==0?"smart_toggle":"editor_hint"));editButtons.add(button);page(button);x+=widths[i]+2;}
            rotate=ButtonWidget.panel(156,37,32,16,Component.literal(DIRECTIONS[direction]),b->{direction=(direction+1)%8;});editButtons.add(rotate);page(rotate);
            bendButton=ButtonWidget.panel(192,37,32,16,tr("bend."+bend),b->{bend=(bend+1)%3;});editButtons.add(bendButton);page(bendButton);
            smartHint=new LabelWidget(156,40,68,10,tr("smart_drag")).withDarkColor();page(smartHint);
            board=new Board();page(board);
        }else if(page==1){
            recipeList=new ScrollWidget(8,56,216,80).withScrollSpeed(24);recipeList.withSurface(OritechSurface.PANEL_INSET);page(recipeList);refreshRecipes();
        }else if(page==2){
            page(new LabelWidget(9,37,215,18,tr("port_hint")).withWrap(true).withDarkColor());String[] sides={"front","left","right","back","top","bottom"};
            for(int i=0;i<6;i++){int side=i;var button=ButtonWidget.panel(8+(i%2)*110,58+(i/2)*26,106,23,Component.empty(),b->{var modes=menu.view.getIntArray("sides");int mode=modes.length==6?modes[side]:0;send(3,side,(mode+1)%5,0,"");});
                button.withTooltip(tr("side."+sides[i]),tr("port_hint"));sideButtons.add(button);page(button);}
        }else{
            page(new LabelWidget(9,37,215,18,tr("magnet_hint")).withDarkColor());
            magnetSlot(ColliderMenu.MAGNET_X,ColliderMenu.MAGNET_Y,Collider.MAGNET_SLOT,"magnet_slot");
            for(int i=0;i<5;i++)magnetSlot(ColliderMenu.UPGRADE_X+i*24,ColliderMenu.UPGRADE_Y,Collider.MAGNET_ADDONS+i,"magnet_addons");
            magnetStats=new LabelWidget(9,86,215,44,Component.empty()).withWrap(true).withDarkColor();page(magnetStats);
        }
    }
    private void magnetSlot(int x,int y,int slot,String hint){page(new ItemSlotWidget(x,y){@Override public void tick(){setTooltip(menu.collider.inventory.getItem(slot).isEmpty()?List.of(tr(hint)):null);}});}
    private void refreshRecipes(){if(recipeList==null)return;recipeList.getChildren().clear();int y=0;String needle=query.toLowerCase(Locale.ROOT);
        for(var holder:menu.collider.recipes()){
            var result=holder.value().getResults().getFirst();if(!result.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle))continue;
            final int rowY=y;final var selected=holder;recipeList.addChild(new UIComponent(0,rowY,204,23){
                {withTooltip(result.getHoverName(),tr("required",holder.value().getTime()));}
                @Override public boolean handleClick(double mx,double my,int button){if(button==0&&!menu.view.getBoolean("busy"))send(8,0,0,0,selected.id().toString());return true;}
                @Override protected void renderContent(GuiGraphics g,int mx,int my,float delta){boolean locked=selected.id().toString().equals(menu.view.getString("locked"));g.fill(x,y,x+width,y+height,locked?0xff365647:0xff43494e);
                    g.renderItem(result,x+3,y+3);g.drawString(font,font.plainSubstrByWidth(result.getHoverName().getString(),142),x+24,y+3,0xffeeeeee,false);
                    g.drawString(font,tr("required",selected.value().getTime()),x+24,y+13,0xffc5d2d9,false);
                }
            });y+=25;
        }recipeList.setContentDimensions(204,Math.max(23,y));
    }
    private void send(int action,int cell,int dir,int curve,String recipe){PacketDistributor.sendToServer(new ColliderPackets.Control(menu.containerId,action,menu.view.getInt("revision"),cell,dir,curve,recipe));}
    private boolean editable(){return !menu.view.getBoolean("busy")&&!menu.view.getBoolean("enabled")&&menu.getCarried().isEmpty();}
    @Override protected void containerTick(){super.containerTick();for(var component:components)component.tick();
        if(page!=menu.page){page=menu.page;buildPage();}
        var data=menu.view;boolean busy=data.getBoolean("busy"),allowed=menu.getCarried().isEmpty()&&!minecraft.player.isSpectator();
        run.setLabel(tr(data.getBoolean("enabled")?"stop":"start"));run.setActive(allowed);cancel.setActive(allowed&&busy);
        lineMaterial.setActive(editable());guideMaterial.setActive(editable());lineMaterial.withTextColor(!smart&&manualSource==Collider.STRAIGHT_PARTS?0xff237255:ButtonWidget.DEFAULT_TEXT_COLOR);guideMaterial.withTextColor(!smart&&manualSource==Collider.PART_IN?0xff237255:ButtonWidget.DEFAULT_TEXT_COLOR);
        automatic.setActive(allowed&&!busy&&!data.getString("locked").isEmpty());lock.setActive(allowed&&!busy&&data.getString("locked").isEmpty()&&!data.getString("recipe").isEmpty());
        automatic.setLabel(tr(data.getString("locked").isEmpty()?"auto":"locked"));eject.setLabel(tr(data.getBoolean("eject")?"eject_on":"eject_off"));eject.setActive(allowed);
        for(var button:editButtons)button.setActive(editable());if(page==0){for(int i=0;i<4;i++)editButtons.get(i).withTextColor(i==tool?0xff237255:ButtonWidget.DEFAULT_TEXT_COLOR);
            editButtons.getFirst().setLabel(tr(smart?"smart":"manual"));rotate.setLabel(Component.literal(DIRECTIONS[direction]));bendButton.setLabel(tr("bend."+bend));
            rotate.setVisible(!smart&&tool==0||tool>=2);bendButton.setVisible(!smart&&tool==0&&manualSource==Collider.PART_IN);smartHint.setVisible(smart&&tool==0);
        }
        if(magnetStats!=null){var settings=MagnetSupport.Settings.load(data.getCompound("magnet"));var energy=String.format(Locale.ROOT,"%,d / %,d FE",data.getLong("magneticEnergy"),settings.capacity());
            magnetStats.setText(settings.capacity()<=0?tr("magnet_missing"):tr("magnet_energy",energy).append("\n").append(settings.enabled()?tr("magnet_efficiency",AddonReadout.multiplier(settings.efficiency(),false)):tr("magnet_disabled")));
            magnetStats.withTooltip(tr("magnet_slot"),tr("magnet_energy",energy),tr("magnet_need",data.getLong("magnetNeeded")),tr("magnet_spent",data.getLong("magneticSpent")));
        }
        String[] sideNames={"front","left","right","back","top","bottom"};var sides=data.getIntArray("sides");for(int i=0;i<sideButtons.size();i++){sideButtons.get(i).setLabel(tr("side."+sideNames[i]).append(": ").append(tr("mode."+(sides.length==6?sides[i]:0))));sideButtons.get(i).setActive(allowed);}
        var speedText=tr("speed",data.getLong("speed"));speed.setText(Component.literal(font.plainSubstrByWidth(speedText.getString(),72)));speed.withTooltip(speedText,tr("required",data.getLong("required")));int status=data.getInt("status");state.setText(tr("status."+status));state.withTooltip(tr("status."+status),tr("spent",data.getLong("spent")));
        if(page==0&&menu.editFailure>0){var error=tr("smart_error."+menu.editFailure);state.setText(error);state.withTooltip(error,
            tr("smart_motors",menu.editMotors,menu.collider.partCount(Track.MOTOR)),tr("smart_guides",menu.editGuides,menu.collider.partCount(Track.RING)));}
        var id=ResourceLocation.tryParse(data.getString("recipe"));var found=menu.collider.recipes().stream().filter(r->r.id().equals(id)).findFirst();
        Component text=found.map(r->r.value().getResults().getFirst().getHoverName()).orElse(Component.empty());recipeName.setText(Component.literal(font.plainSubstrByWidth(text.getString(),72)));recipeName.withTooltip(text);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){super.render(g,mouseX,mouseY,partial);
        if(recipeList!=null&&recipeList.isMouseOver(mouseX-leftPos,mouseY-topPos)){var row=recipeList.getTopmostHovered(mouseX-leftPos,mouseY-topPos);if(row!=null&&row.hasTooltip())g.renderComponentTooltip(font,row.getTooltip(),mouseX,mouseY);}}
    private void addExpectedInput(int slot,int x,int y){addComponent(new UIComponent(x,y,16,16){
        @Override protected void renderContent(GuiGraphics g,int mx,int my,float delta){setTooltip(null);if(!menu.collider.inventory.getItem(slot).isEmpty())return;
            String locked=menu.view.getString("locked");if(locked.isEmpty())return;
            var recipe=menu.collider.recipes().stream().filter(r->r.id().toString().equals(locked)).findFirst();if(recipe.isEmpty())return;
            int ingredient=menu.view.getBoolean("swapped")?1-slot:slot;var examples=recipe.get().value().getInputs().get(ingredient).getItems();if(examples.length==0)return;
            var example=examples[(int)((menu.collider.getLevel().getGameTime()/30)%examples.length)];g.renderItem(example,x,y);g.fill(x,y,x+16,y+16,0x773b4146);withTooltip(tr(slot==0?"input_a":"input_b"),example.getHoverName());
        }
    });}

    private final class Board extends UIComponent {
        private boolean panning,painting,removing;
        private final List<Integer> stroke=new ArrayList<>();
        private int strokeRevision,lastPaint=-1;
        private SmartTrack.Result preview;
        private int previewHash;
        Board(){super(BOARD_X,BOARD_Y,BOARD_W,BOARD_H);}
        private int at(double mx,double my){int gx=(int)Math.floor((mx-x)/cellSize+panX),gy=(int)Math.floor((my-y)/cellSize+panY);return gx<0||gy<0||gx>=Track.SIZE||gy>=Track.SIZE?-1:Track.cell(gx,gy);}
        private int sx(double gx){return x+(int)Math.round((gx-panX+.5)*cellSize);}
        private int sy(double gy){return y+(int)Math.round((gy-panY+.5)*cellSize);}
        @Override protected void renderContent(GuiGraphics g,int mx,int my,float delta){
            int hover=isMouseOver(mx,my)?at(mx,my):-1;
            updatePreview(hover);
            g.fill(x,y,x+width,y+height,0xff28323a);g.enableScissor(leftPos+x,topPos+y,leftPos+x+width,topPos+y+height);
            for(int gx=(int)Math.floor(panX);gx<=Math.ceil(panX+width/cellSize);gx++){int screen=sx(gx)- (int)(cellSize/2);g.fill(screen,y,screen+1,y+height,0xff3a454d);}
            for(int gy=(int)Math.floor(panY);gy<=Math.ceil(panY+height/cellSize);gy++){int screen=sy(gy)-(int)(cellSize/2);g.fill(x,screen,x+width,screen+1,0xff3a454d);}
            for(var entry:menu.layout.entrySet()){
                int pos=entry.getKey();var node=entry.getValue();int cx=sx(Track.x(pos)),cy=sy(Track.y(pos)),radius=Math.max(2,(int)(cellSize*.4));
                if(cx+radius<x||cx-radius>x+width||cy+radius<y||cy-radius>y+height)continue;
                int color=node.kind()==Track.A?0xff6ec6ed:node.kind()==Track.B?0xfff3ba68:node.kind()==Track.MOTOR?0xff83d2a5:0xffd0d8de;
                g.fill(cx-radius,cy-radius,cx+radius+1,cy+radius+1,0xff46555f);
                line(g,cx,cy,cx+Track.DX[node.front()]*radius,cy+Track.DY[node.front()]*radius,color);
                line(g,cx,cy,cx+Track.DX[node.back()]*radius,cy+Track.DY[node.back()]*radius,color);
                if(node.kind()!=Track.RING&&cellSize>=10){String mark=node.kind()==Track.A?"A":node.kind()==Track.B?"B":node.kind()==Track.MOTOR?"M":"S";g.drawString(font,mark,cx-font.width(mark)/2,cy-4,color,false);}
                if(pos==menu.view.getInt("problem")&&menu.view.getInt("status")>=Collider.TRACK_BASE)g.renderOutline(cx-radius,cy-radius,radius*2+1,radius*2+1,0xffec7878);
            }
            if(preview!=null){boolean enough=preview.needed(menu.layout,Track.MOTOR)<=menu.collider.partCount(Track.MOTOR)&&preview.needed(menu.layout,Track.RING)<=menu.collider.partCount(Track.RING);int color=preview.valid()&&enough?0xff99e6bf:0xffed8b84;
                for(var entry:preview.nodes().entrySet()){int cx=sx(Track.x(entry.getKey())),cy=sy(Track.y(entry.getKey())),r=Math.max(2,(int)(cellSize*.4));var node=entry.getValue();
                    g.pose().pushPose();g.pose().translate(0,0,10);g.fill(cx-r,cy-r,cx+r+1,cy+r+1,0xff35434c);
                    g.renderOutline(cx-r,cy-r,r*2+1,r*2+1,color);line(g,cx,cy,cx+Track.DX[node.front()]*r,cy+Track.DY[node.front()]*r,color);line(g,cx,cy,cx+Track.DX[node.back()]*r,cy+Track.DY[node.back()]*r,color);
                    if(node.kind()==Track.MOTOR&&cellSize>=10)g.drawString(font,"M",cx-font.width("M")/2,cy-4,color,false);g.pose().popPose();
                }
                if(!preview.valid())for(int pos:stroke.isEmpty()?hover>=0?List.of(hover):List.<Integer>of():stroke){int cx=sx(Track.x(pos)),cy=sy(Track.y(pos));g.renderOutline(cx-(int)cellSize/2,cy-(int)cellSize/2,(int)cellSize,(int)cellSize,color);}
            }else for(int pos:stroke){int cx=sx(Track.x(pos)),cy=sy(Track.y(pos));g.renderOutline(cx-(int)cellSize/2,cy-(int)cellSize/2,(int)cellSize,(int)cellSize,0xff91c6e5);}
            if(menu.view.getBoolean("busy")&&menu.view.getLong("speed")>0){int cx=sx(menu.view.getDouble("x")),cy=sy(menu.view.getDouble("y"));g.fill(cx-2,cy-2,cx+3,cy+3,0xff80e6ff);}
            g.disableScissor();
            if(hover>=0){var node=preview!=null&&preview.valid()?preview.nodes().getOrDefault(hover,menu.layout.get(hover)):menu.layout.get(hover);var label=node==null?tr("empty"):nodeName(node.kind());withTooltip(label,Component.literal((Track.x(hover)+1)+", "+(Track.y(hover)+1)),tr("editor_hint"));
                if(preview!=null){if(preview.valid()){
                    addTooltipLine(tr("smart_motors",preview.needed(menu.layout,Track.MOTOR),menu.collider.partCount(Track.MOTOR)));addTooltipLine(tr("smart_guides",preview.needed(menu.layout,Track.RING),menu.collider.partCount(Track.RING)));
                    if(preview.nodes().entrySet().stream().anyMatch(e->menu.layout.containsKey(e.getKey())&&menu.layout.get(e.getKey()).kind()!=e.getValue().kind()))addTooltipLine(tr("smart_recycle"));
                }else addTooltipLine(tr("smart_error."+preview.failure().ordinal()));}
            }else setTooltip(null);
        }
        private void updatePreview(int hover){if(!smart||tool!=0||removing||!editable()||stroke.isEmpty()&&hover<0){preview=null;return;}
            int gap=Math.clamp(menu.view.getInt("maxGap"),1,Track.SIZE);int hash=Objects.hash(menu.view.getInt("revision"),stroke,hover,direction,gap);if(preview!=null&&hash==previewHash)return;previewHash=hash;
            int[] path=stroke.isEmpty()?new int[]{hover}:stroke.stream().mapToInt(Integer::intValue).toArray();preview=SmartTrack.mixed(menu.layout,path,direction,gap);
        }
        @Override public boolean handleClick(double mx,double my,int button){
            if(button==2||button==0&&hasShiftDown()){panning=true;return true;}
            if(!editable())return true;int pos=at(mx,my);if(pos<0)return true;
            if(tool>=2){if(button==0)send(4+tool,pos,direction,0,"");return true;}
            if(button==0||button==1){painting=true;removing=tool==1||button==1;stroke.clear();strokeRevision=menu.view.getInt("revision");stroke.add(pos);lastPaint=pos;preview=null;return true;}return false;
        }
        @Override public boolean handleDrag(double mx,double my,double dx,double dy,int button){
            if(panning){panX=Math.clamp(panX-dx/cellSize,0,Math.max(0,Track.SIZE-width/cellSize));panY=Math.clamp(panY-dy/cellSize,0,Math.max(0,Track.SIZE-height/cellSize));return true;}
            if(painting&&editable()){if(!isMouseOver(mx,my))return true;int pos=at(mx,my);if(pos>=0&&stroke.size()<256){
                int from=lastPaint<0?pos:lastPaint,steps=Math.max(Math.abs(Track.x(pos)-Track.x(from)),Math.abs(Track.y(pos)-Track.y(from)));
                for(int i=0;i<=steps&&stroke.size()<256;i++){double t=steps==0?0:(double)i/steps;append(Track.cell((int)Math.round(Track.x(from)+(Track.x(pos)-Track.x(from))*t),(int)Math.round(Track.y(from)+(Track.y(pos)-Track.y(from))*t)));}lastPaint=pos;
            }return true;}return false;
        }
        @Override public boolean handleMouseRelease(double mx,double my,int button){
            panning=false;if(painting){painting=false;if(!stroke.isEmpty()&&editable())PacketDistributor.sendToServer(new ColliderPackets.Paint(menu.containerId,strokeRevision,stroke.stream().mapToInt(Integer::intValue).toArray(),direction,bend,removing,smart&&!removing,manualSource));stroke.clear();preview=null;removing=false;return true;}return false;
        }
        private void append(int pos){if(!stroke.isEmpty()&&stroke.getLast()==pos)return;
            if(smart&&!removing){int existing=stroke.indexOf(pos);if(existing>=0&&!(existing==0&&stroke.size()>3)){while(stroke.size()>existing+1)stroke.removeLast();return;}}
            stroke.add(pos);
        }
        @Override public boolean handleMouseScroll(double mx,double my,double delta){if(!isMouseOver(mx,my))return false;double gx=(mx-x)/cellSize+panX,gy=(my-y)/cellSize+panY;cellSize=Math.clamp(cellSize+(delta>0?2:-2),6,24);panX=Math.clamp(gx-(mx-x)/cellSize,0,Math.max(0,Track.SIZE-width/cellSize));panY=Math.clamp(gy-(my-y)/cellSize,0,Math.max(0,Track.SIZE-height/cellSize));return true;}
    }
    private static Component nodeName(int kind){return kind==Track.A?tr("emitter_a"):kind==Track.B?tr("emitter_b"):new ItemStack(kind==Track.MOTOR?BlockContent.ACCELERATOR_MOTOR:kind==Track.SENSOR?BlockContent.ACCELERATOR_SENSOR:BlockContent.ACCELERATOR_RING).getHoverName();}
    private static void line(GuiGraphics g,int x0,int y0,int x1,int y1,int color){int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,error=dx+dy;
        while(true){g.fill(x0,y0,x0+1,y0+1,color);if(x0==x1&&y0==y1)break;int e=2*error;if(e>=dy){error+=dy;x0+=sx;}if(e<=dx){error+=dx;y0+=sy;}}
    }
}
