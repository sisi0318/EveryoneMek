package dev.everyonemek.gravity.expansion;
import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.text.GuiTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import static dev.everyonemek.gravity.expansion.LinkPanelGeometry.*;

/** Native Mek frame and controls around a movable, zoomable connection canvas. */
public final class LinkPanelScreen extends GuiMekanism<LinkPanelMenu> {
    private static final int POWER=0xFFDBAA55,ROUTE=0xFF6BB5DD,BAD=0xFFC96A71;
    private final Map<BlockPos,Point> positions=new HashMap<>();
    private GuiTextField search;private String filter="";private BlockPos selected,moving;
    private record Port(BlockPos pos,int type,boolean output){}
    private record Edge(BlockPos from,BlockPos to,int type,boolean valid){}
    private Port wire;private Edge selectedEdge;private int wireRevision;
    private double zoom=1,panX=16,panY=14,dragX,dragY,startMouseX,startMouseY;
    private boolean panning,firstLayout=true;private int cx,cy,cw,ch;private boolean sidebar;
    public LinkPanelScreen(LinkPanelMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    @Override protected void init(){imageWidth=Math.min(660,Math.max(220,width-16));imageHeight=Math.min(410,Math.max(200,height-16));sidebar=imageWidth>=470;super.init();canvasBounds();}
    private void canvasBounds(){cx=leftPos+8;cy=topPos+68;cw=imageWidth-16-(sidebar?150:0);ch=imageHeight-128;}
    @Override protected void addGuiElements(){super.addGuiElements();
        addRenderableWidget(new MekanismButton(this,8,24,62,16,ModuleContent.text("panel_refresh"),(b,x,y)->{send(0,null,null,menu.revision);return true;}));
        addRenderableWidget(new MekanismButton(this,76,24,76,16,ModuleContent.text("panel_layout"),(b,x,y)->{layout(true);fit();return true;}));
        if(imageWidth>=310)addRenderableWidget(new MekanismButton(this,158,24,62,16,ModuleContent.text("panel_fit"),(b,x,y)->{fit();return true;}));
        addRenderableWidget(new MekanismButton(this,imageWidth-60,24,52,16,ModuleContent.text("panel_close"),(b,x,y)->{onClose();return true;}));
        search=addRenderableWidget(new GuiTextField(this,8,46,imageWidth-16,16));search.setMaxLength(64);search.setText(filter);search.setResponder(text->{filter=text.toLowerCase(Locale.ROOT);wire=null;selectedEdge=null;});search.setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(ModuleContent.text("panel_search")));
        int step=(imageWidth-16)/6;
        for(int i=0;i<6;i++){int action=i;addRenderableWidget(new MekanismButton(this,8+i*step,imageHeight-27,step-3,17,buttonText(i),(b,x,y)->{
            var d=selected();if(d!=null)send(action<4?4+action:action==4?2:3,d.pos(),null,menu.revision);return true;
        }){{active=false;}@Override public void tick(){super.tick();var d=selected();active=d!=null&&(action<4?d.node():action==4?!d.core()&&d.source()!=null:d.node()&&d.peer()!=null);setMessage(buttonText(action));}});}
    }
    private Component buttonText(int action){if(action>=4)return ModuleContent.text(action==4?"panel_remove_power":"panel_remove_route");var d=selected();return ModuleContent.text("panel_channel",ModuleContent.text("resource_"+action),d!=null&&(d.channels()&(1<<action))!=0?"✓":"—");}
    private List<LinkPanelNetwork.Device> visible(){return menu.devices.stream().filter(d->filter.isEmpty()||d.name().getString().toLowerCase(Locale.ROOT).contains(filter)||d.pos().toShortString().contains(filter)).toList();}
    private LinkPanelNetwork.Device device(BlockPos pos){if(pos==null)return null;return menu.devices.stream().filter(d->d.pos().equals(pos)).findFirst().orElse(null);}
    private LinkPanelNetwork.Device selected(){return device(selected);}
    private void layout(boolean reset){if(reset)positions.clear();var devices=menu.devices;positions.keySet().removeIf(p->device(p)==null);int[] columns=devices.stream().mapToInt(d->d.core()?0:d.node()&&d.peer()==null&&d.receiver()?2:1).toArray();var points=LinkPanelGeometry.layout(columns);
        for(int i=0;i<devices.size();i++)if(!positions.containsKey(devices.get(i).pos())){var point=points[i];while(positions.containsValue(point))point=new Point(point.x(),point.y()+ROW);positions.put(devices.get(i).pos(),point);}
        if(firstLayout&&!devices.isEmpty()){fit();firstLayout=false;}
    }
    private void fit(){var list=visible();if(list.isEmpty())return;double minX=Double.MAX_VALUE,minY=minX,maxX=-minX,maxY=-minX;
        for(var d:list){var p=positions.get(d.pos());if(p==null)continue;minX=Math.min(minX,p.x());minY=Math.min(minY,p.y());maxX=Math.max(maxX,p.x()+WIDTH);maxY=Math.max(maxY,p.y()+HEIGHT);}
        if(minX==Double.MAX_VALUE)return;zoom=Math.clamp(Math.min((cw-28)/(maxX-minX),(ch-24)/(maxY-minY)),.45,1.25);panX=14-minX*zoom;panY=12-minY*zoom;
    }
    private Point graph(double x,double y){return new Point((x-cx-panX)/zoom,(y-cy-panY)/zoom);}
    private Point screen(Point p){return new Point(cx+panX+p.x()*zoom,cy+panY+p.y()*zoom);}
    private boolean canvas(double x,double y){return x>=cx&&x<cx+cw&&y>=cy&&y<cy+ch;}
    private Point port(Port p){var at=positions.get(p.pos());var d=device(p.pos());return new Point(at.x()+(p.output()?WIDTH:0),at.y()+(p.type()==1?33:d!=null&&d.core()?22:13));}
    private List<Port> ports(LinkPanelNetwork.Device d){var list=new ArrayList<Port>();list.add(new Port(d.pos(),0,d.core()));if(d.node()){list.add(new Port(d.pos(),1,false));list.add(new Port(d.pos(),1,true));}return list;}
    private Port hitPort(Point point){for(var d:visible())for(var p:ports(d)){var at=port(p);if(Math.pow(at.x()-point.x(),2)+Math.pow(at.y()-point.y(),2)<81)return p;}return null;}
    private LinkPanelNetwork.Device hitCard(Point point){var list=visible();for(int i=list.size()-1;i>=0;i--){var d=list.get(i);var at=positions.get(d.pos());if(at!=null&&contains(at,point))return d;}return null;}
    private List<Edge> edges(){var result=new ArrayList<Edge>();var shown=visible().stream().map(d->d.pos()).collect(java.util.stream.Collectors.toSet());for(var d:visible()){
        if(d.source()!=null&&shown.contains(d.source()))result.add(new Edge(d.source(),d.pos(),0,d.powerValid()));
        if(d.peer()!=null&&shown.contains(d.peer()))result.add(new Edge(d.pos(),d.peer(),1,d.routeValid()));}return result;}
    private boolean compatible(Port from,Port to){if(from==null||to==null||!from.output()||to.output()||from.type()!=to.type()||from.pos().equals(to.pos()))return false;var a=device(from.pos());var b=device(to.pos());if(a==null||b==null)return false;
        if(from.type()==1)return a.node()&&b.node();return a.core()&&!b.core()&&!(b.kind()==ModuleKind.CAPTOR.ordinal()+2&&a.kind()!=1||b.kind()==ModuleKind.FORGE.ordinal()+2&&a.kind()!=0);}
    private void connect(Port target){if(compatible(wire,target))send(1,wire.pos(),target.pos(),wireRevision);wire=null;}
    private void send(int action,BlockPos from,BlockPos to,int revision){PacketDistributor.sendToServer(new LinkPanelNetwork.Action(menu.containerId,menu.session,revision,action,from,to));}
    private static void line(GuiGraphics g,Point a,Point b,float width,int color){double dx=b.x()-a.x(),dy=b.y()-a.y(),length=Math.hypot(dx,dy);if(length<.001)return;float nx=(float)(-dy/length*width/2),ny=(float)(dx/length*width/2);var m=g.pose().last().pose();VertexConsumer v=g.bufferSource().getBuffer(RenderType.gui());
        // Match GuiGraphics.fill winding so the native GUI RenderType can retain face culling.
        v.addVertex(m,(float)a.x()+nx,(float)a.y()+ny,0).setColor(color);v.addVertex(m,(float)b.x()+nx,(float)b.y()+ny,0).setColor(color);v.addVertex(m,(float)b.x()-nx,(float)b.y()-ny,0).setColor(color);v.addVertex(m,(float)a.x()-nx,(float)a.y()-ny,0).setColor(color);}
    private void curve(GuiGraphics g,Point a,Point b,int color,boolean dashed,boolean highlight){Point previous=screen(a);for(int i=1;i<=SEGMENTS;i++){var next=screen(LinkPanelGeometry.curve(a,b,i/(double)SEGMENTS));if(!dashed||i%3!=0)line(g,previous,next,highlight?3:1.5F,color);previous=next;}
        var tip=screen(b);line(g,new Point(tip.x()-5,tip.y()-3),tip,1.5F,color);line(g,new Point(tip.x()-5,tip.y()+3),tip,1.5F,color);}
    private Edge hitEdge(Point p){for(var e:edges()){var a=port(new Port(e.from(),e.type(),true));var b=port(new Port(e.to(),e.type(),false));var previous=a;for(int i=1;i<=SEGMENTS;i++){var next=LinkPanelGeometry.curve(a,b,i/(double)SEGMENTS);if(segmentDistance(p,previous,next)<Math.pow(5/zoom,2))return e;previous=next;}}return null;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){super.renderBg(g,partial,mx,my);canvasBounds();layout(false);
        g.fill(cx-1,cy-1,cx+cw+1,cy+ch+1,0xFF70747A);g.fill(cx,cy,cx+cw,cy+ch,0xFF20272D);g.enableScissor(cx,cy,cx+cw,cy+ch);
        int spacing=Math.max(12,(int)(24*zoom));for(int x=cx+Math.floorMod((int)panX,spacing);x<cx+cw;x+=spacing)g.fill(x,cy,x+1,cy+ch,0xFF2B343C);for(int y=cy+Math.floorMod((int)panY,spacing);y<cy+ch;y+=spacing)g.fill(cx,y,cx+cw,y+1,0xFF2B343C);
        for(var e:edges())curve(g,port(new Port(e.from(),e.type(),true)),port(new Port(e.to(),e.type(),false)),e.valid()?(e.type()==0?POWER:ROUTE):BAD,!e.valid(),e.equals(selectedEdge));g.flush();
        for(var d:visible()){var at=screen(positions.get(d.pos()));if(at.x()+WIDTH*zoom<cx||at.x()>cx+cw||at.y()+HEIGHT*zoom<cy||at.y()>cy+ch)continue;
            g.pose().pushPose();g.pose().translate(at.x(),at.y(),0);g.pose().scale((float)zoom,(float)zoom,1);int border=d.pos().equals(selected)?0xFFF0DC96:0xFF73828C;
            g.fill(0,0,WIDTH,HEIGHT,border);g.fill(1,1,WIDTH-1,HEIGHT-1,0xFF465159);g.fill(1,1,WIDTH-1,11,0xFF333D45);
            g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(d.item())),7,15);g.drawString(font,font.plainSubstrByWidth(d.name().getString(),WIDTH-12),6,2,0xFFE6EAED,false);
            g.drawString(font,font.plainSubstrByWidth(d.pos().toShortString(),WIDTH-31),28,17,0xFFCCD3D6,false);g.drawString(font,font.plainSubstrByWidth(state(d).getString(),WIDTH-31),28,31,d.active()?0xFF8FE3B0:0xFFBBC0C5,false);g.pose().popPose();
            for(var p:ports(d)){var dot=screen(port(p));boolean match=wire==null||compatible(wire,p)||p.equals(wire);int color=match?(p.type()==0?POWER:ROUTE):0xFF64717A;g.fill((int)dot.x()-4,(int)dot.y()-4,(int)dot.x()+4,(int)dot.y()+4,0xFF13191E);g.fill((int)dot.x()-3,(int)dot.y()-3,(int)dot.x()+3,(int)dot.y()+3,color);}
        }
        if(wire!=null&&device(wire.pos())!=null)curve(g,port(wire),graph(mx,my),wire.type()==0?POWER:ROUTE,false,true);g.flush();g.disableScissor();
        if(sidebar)drawDetails(g,cx+cw+10,cy,134);
        if(wire==null&&moving==null&&!panning&&canvas(mx,my)){var p=hitPort(graph(mx,my));var d=hitCard(graph(mx,my));if(p!=null)setTooltipForNextRenderPass(ModuleContent.text(p.type()==0?(p.output()?"panel_power_out":"panel_power_in"):(p.output()?"panel_route_out":"panel_route_in")));else if(d!=null)setTooltipForNextRenderPass(List.of(d.name().getVisualOrderText(),Component.literal(d.pos().toShortString()).getVisualOrderText(),state(d).getVisualOrderText()));}
    }
    private Component state(LinkPanelNetwork.Device d){return d.kind()==0?dev.everyonemek.gravity.Content.text(d.state()):d.kind()==1?dev.everyonemek.gravity.solar.SolarContent.text(d.state()):ModuleContent.text(d.state());}
    private void drawDetails(GuiGraphics g,int x,int y,int width){g.fill(x,y,x+width,y+ch,0xFF2D363D);var d=selected();List<Component> lines=d==null?List.of(ModuleContent.text("panel_legend_power"),ModuleContent.text("panel_legend_route"),ModuleContent.text("panel_help_drag"),ModuleContent.text("panel_help_pan"),ModuleContent.text("panel_help_wire")):List.of(d.name(),Component.literal(d.pos().toShortString()),state(d),ModuleContent.text("source_pos",d.source()==null?"—":d.source().toShortString()),ModuleContent.text("peer_pos",d.peer()==null?"—":d.peer().toShortString()),ModuleContent.text("panel_help_wire"));
        int offset=7;for(var text:lines){for(var line:font.split(text,width-12)){if(offset+9>ch)break;g.drawString(font,line,x+6,y+offset,0xFFD9DFE3,false);offset+=11;}offset+=6;}}
    @Override protected void drawForegroundText(GuiGraphics g,int x,int y){renderTitleText(g);int y0=imageHeight-55;String text=ModuleContent.text("panel_count",visible().size(),menu.total,menu.range).getString();g.drawString(font,font.plainSubstrByWidth(text,imageWidth-16),8,y0,titleTextColor(),false);
        var label=wire!=null?ModuleContent.text("panel_choose_target"):selectedEdge!=null?ModuleContent.text("panel_delete_wire"):ModuleContent.text(menu.feedback);g.drawString(font,font.plainSubstrByWidth(label.getString(),imageWidth-16),8,y0+13,titleTextColor(),false);
        if(search!=null&&search.getText().isEmpty()&&!search.isTextFieldFocused())g.drawString(font,ModuleContent.text("panel_search"),13,50,0xFF80858A,false);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(!canvas(x,y))return super.mouseClicked(x,y,button);search.setFocused(false);if(button==1){wire=null;selectedEdge=null;return true;}var point=graph(x,y);var p=hitPort(point);startMouseX=x;startMouseY=y;
        if(button==0&&p!=null){if(p.output()){wire=p;wireRevision=menu.revision;selected=p.pos();selectedEdge=null;}else if(wire!=null)connect(p);else selected=p.pos();return true;}
        var d=hitCard(point);if(button==0&&d!=null){if(wire!=null){connect(new Port(d.pos(),wire.type(),false));return true;}selected=d.pos();selectedEdge=null;if(point.y()-positions.get(d.pos()).y()<12){moving=d.pos();dragX=point.x()-positions.get(moving).x();dragY=point.y()-positions.get(moving).y();}return true;}
        if(button==0){selectedEdge=hitEdge(point);if(selectedEdge!=null){selected=selectedEdge.type()==0?selectedEdge.to():selectedEdge.from();return true;}}
        panning=true;return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(moving!=null){var p=graph(x,y);positions.put(moving,new Point(p.x()-dragX,p.y()-dragY));return true;}if(panning){panX+=dx;panY+=dy;return true;}if(wire!=null)return true;return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(moving!=null||panning){moving=null;panning=false;return true;}if(wire!=null&&button==0&&Math.hypot(x-startMouseX,y-startMouseY)>4&&canvas(x,y)){var p=hitPort(graph(x,y));if(p!=null&&!p.output())connect(p);else{var d=hitCard(graph(x,y));if(d!=null)connect(new Port(d.pos(),wire.type(),false));}return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(!canvas(x,y))return super.mouseScrolled(x,y,horizontal,vertical);var before=graph(x,y);zoom=Math.clamp(zoom*Math.pow(1.12,vertical),.4,1.6);panX=x-cx-before.x()*zoom;panY=y-cy-before.y()*zoom;return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(search!=null&&search.isTextFieldFocused())return super.keyPressed(key,scan,modifiers);if(key==GLFW.GLFW_KEY_ESCAPE&&wire!=null){wire=null;return true;}if(key==GLFW.GLFW_KEY_DELETE&&selectedEdge!=null){var e=selectedEdge;send(e.type()==0?2:3,e.type()==0?e.to():e.from(),e.type()==0?e.from():e.to(),menu.revision);selectedEdge=null;return true;}return super.keyPressed(key,scan,modifiers);}
}
