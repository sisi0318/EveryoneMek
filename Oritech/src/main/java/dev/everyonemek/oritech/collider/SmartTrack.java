package dev.everyonemek.oritech.collider;

import java.util.*;

/** Plans a complete edit without touching inventory or the caller's beamline. */
public final class SmartTrack {
    public enum Failure { NONE,BAD_PATH,SHARP_TURN,OCCUPIED_CORNER,NEED_RING,EMITTER_DIRECTION,AMBIGUOUS,NOT_ENOUGH_PARTS }
    public record Result(Map<Integer,Track.Node> nodes,int required,Failure failure,int problem){
        public boolean valid(){return failure==Failure.NONE;}
    }
    private record Link(int direction,int target,Track.Node adjustment){}
    public static Result plan(Map<Integer,Track.Node> existing,int[] stroke,int kind,int fallback,int maxGap){
        if(stroke.length==0||stroke.length>256||kind<Track.RING||kind>Track.SENSOR||fallback<0||fallback>7)return fail(Failure.BAD_PATH,-1);
        var path=new ArrayList<Integer>();for(int pos:stroke){if(!Track.inBounds(pos))return fail(Failure.BAD_PATH,pos);if(path.isEmpty()||path.getLast()!=pos)path.add(pos);}
        boolean closed=path.size()>2&&path.getFirst().equals(path.getLast());if(closed)path.removeLast();
        if(new HashSet<>(path).size()!=path.size())return fail(Failure.BAD_PATH,path.getLast());
        for(int i=1;i<path.size();i++)if(!adjacent(path.get(i-1),path.get(i)))return fail(Failure.BAD_PATH,path.get(i));
        if(closed&&!adjacent(path.getLast(),path.getFirst()))return fail(Failure.BAD_PATH,path.getLast());
        // A rectangular mouse corner becomes the diagonal between its two neighbours.
        // Existing components and built-in emitters are never silently removed to make room.
        for(int pass=0;pass<256;pass++){
            boolean changed=false;int start=closed?0:1,end=closed?path.size():path.size()-1;
            for(int i=start;i<end;i++){
                int before=path.get((i+path.size()-1)%path.size()),at=path.get(i),after=path.get((i+1)%path.size());
                int incoming=direction(before,at),outgoing=direction(at,after),turn=angle(incoming,outgoing);
                if(turn<=1)continue;
                if(turn!=2||!adjacent(before,after))return fail(Failure.SHARP_TURN,at);
                if(existing.containsKey(at))return fail(Failure.OCCUPIED_CORNER,at);
                if(nodeKind(existing,before,kind)!=Track.RING||nodeKind(existing,after,kind)!=Track.RING)return fail(Failure.NEED_RING,at);
                path.remove(i);changed=true;break;
            }
            if(!changed)break;if(path.size()<3)return fail(Failure.BAD_PATH,path.getFirst());
        }
        var changes=new LinkedHashMap<Integer,Track.Node>();var selected=new HashSet<>(path);maxGap=Math.clamp(maxGap,1,Track.SIZE);
        for(int i=0;i<path.size();i++){
            int pos=path.get(i),type=nodeKind(existing,pos,kind);var current=existing.get(pos);
            Integer prev=i>0?path.get(i-1):closed?path.getLast():null;
            Integer next=i+1<path.size()?path.get(i+1):closed?path.getFirst():null;
            var fixed=new ArrayList<Integer>();if(prev!=null)fixed.add(direction(pos,prev));if(next!=null)fixed.add(direction(pos,next));
            if(fixed.size()==2){
                var target=fromPorts(type,fixed.get(0),fixed.get(1));if(target==null)return fail(type>=Track.A?Failure.EMITTER_DIRECTION:type==Track.RING?Failure.SHARP_TURN:Failure.NEED_RING,pos);
                if(type>=Track.A){if(!samePorts(current,target))return fail(Failure.EMITTER_DIRECTION,pos);target=current;}
                else if(current!=null&&samePorts(current,target))target=current;
                changes.put(pos,target);continue;
            }
            if(type>=Track.A){if(fixed.isEmpty()||hasPort(current,fixed.getFirst())){changes.put(pos,current);continue;}return fail(Failure.EMITTER_DIRECTION,pos);}
            var links=links(existing,selected,pos,fixed,maxGap);
            if(fixed.size()==1){
                int port=fixed.getFirst();links.removeIf(link->fromPorts(type,port,link.direction())==null);
                Link selectedLink=null;
                if(current!=null)for(var link:links)if(hasPort(current,port)&&hasPort(current,link.direction())){selectedLink=link;break;}
                if(selectedLink==null)for(var link:links)if(link.direction()==((port+4)&7)){selectedLink=link;break;}
                if(selectedLink==null&&links.size()==1)selectedLink=links.getFirst();
                if(selectedLink==null&&links.size()>1)return fail(Failure.AMBIGUOUS,pos);
                var target=selectedLink==null?current!=null&&hasPort(current,port)?current:fromPorts(type,port,(port+4)&7):fromPorts(type,port,selectedLink.direction());
                changes.put(pos,target);if(selectedLink!=null&&!addAdjustment(changes,selectedLink))return fail(Failure.AMBIGUOUS,selectedLink.target());
            }else if(links.size()<2){
                int port=links.isEmpty()?(fallback+4)&7:links.getFirst().direction();
                changes.put(pos,current!=null&&(links.isEmpty()||hasPort(current,port))?current:fromPorts(type,port,(port+4)&7));
                if(!links.isEmpty()&&!addAdjustment(changes,links.getFirst()))return fail(Failure.AMBIGUOUS,links.getFirst().target());
            }else{
                var pairs=new ArrayList<List<Link>>();for(int a=0;a<links.size();a++)for(int b=a+1;b<links.size();b++)if(fromPorts(type,links.get(a).direction(),links.get(b).direction())!=null)pairs.add(List.of(links.get(a),links.get(b)));
                List<Link> pair=null;if(current!=null)for(var candidate:pairs)if(hasPort(current,candidate.get(0).direction())&&hasPort(current,candidate.get(1).direction())){pair=candidate;break;}
                if(pair==null&&pairs.size()==1)pair=pairs.getFirst();if(pair==null)return fail(pairs.isEmpty()?Failure.SHARP_TURN:Failure.AMBIGUOUS,pos);
                changes.put(pos,fromPorts(type,pair.get(0).direction(),pair.get(1).direction()));for(var link:pair)if(!addAdjustment(changes,link))return fail(Failure.AMBIGUOUS,link.target());
            }
        }
        int required=0;for(int pos:changes.keySet())if(!existing.containsKey(pos))required++;
        return new Result(Collections.unmodifiableMap(changes),required,Failure.NONE,-1);
    }
    private static boolean addAdjustment(Map<Integer,Track.Node> changes,Link link){if(link.adjustment()==null)return true;var previous=changes.putIfAbsent(link.target(),link.adjustment());return previous==null||previous.equals(link.adjustment());}
    private static List<Link> links(Map<Integer,Track.Node> nodes,Set<Integer> path,int pos,List<Integer> fixed,int maxGap){var result=new ArrayList<Link>();
        for(int direction=0;direction<8;direction++){
            if(fixed.contains(direction))continue;int target=nearest(nodes,path,pos,direction,maxGap);if(target<0||path.contains(target))continue;var node=nodes.get(target);int toward=(direction+4)&7;
            if(acceptsIncoming(node,toward)){result.add(new Link(direction,target,null));continue;}
            if(node.kind()!=Track.RING)continue;
            var connected=new ArrayList<Integer>();for(int port:new int[]{node.front(),node.back()}){
                int neighbour=nearest(nodes,Set.of(),target,port,maxGap);if(neighbour>=0&&neighbour!=pos&&acceptsIncoming(nodes.get(neighbour),(port+4)&7))connected.add(port);
            }
            if(connected.size()>1)continue;var adjustment=fromPorts(Track.RING,connected.isEmpty()?(toward+4)&7:connected.getFirst(),toward);
            if(adjustment!=null)result.add(new Link(direction,target,adjustment));
        }return result;
    }
    private static int nearest(Map<Integer,Track.Node> nodes,Set<Integer> path,int pos,int direction,int maxGap){for(int i=1;i<=maxGap;i++){
        int x=Track.x(pos)+Track.DX[direction]*i,y=Track.y(pos)+Track.DY[direction]*i;if(x<0||x>=Track.SIZE||y<0||y>=Track.SIZE)return -1;
        int candidate=Track.cell(x,y);if(nodes.containsKey(candidate)||path.contains(candidate))return candidate;
    }return -1;}
    private static int nodeKind(Map<Integer,Track.Node> nodes,int pos,int fallback){var node=nodes.get(pos);return node==null?fallback:node.kind();}
    private static boolean hasPort(Track.Node node,int port){return node!=null&&(node.front()==port||node.back()==port);}
    private static boolean acceptsIncoming(Track.Node node,int port){return node!=null&&(node.kind()==Track.MOTOR||node.kind()==Track.SENSOR||hasPort(node,port));}
    private static boolean samePorts(Track.Node a,Track.Node b){return hasPort(a,b.front())&&hasPort(a,b.back());}
    public static Track.Node fromPorts(int kind,int first,int second){
        if(first==second)return null;int facing=(first+4)&7,turn=(second-facing+8)&7;
        if(turn==0)return new Track.Node(kind,facing,0);
        if(kind!=Track.RING)return null;
        return turn==1?new Track.Node(kind,facing,2):turn==7?new Track.Node(kind,facing,1):null;
    }
    private static boolean adjacent(int a,int b){return Math.max(Math.abs(Track.x(a)-Track.x(b)),Math.abs(Track.y(a)-Track.y(b)))==1;}
    private static int direction(int from,int to){int x=Integer.signum(Track.x(to)-Track.x(from)),y=Integer.signum(Track.y(to)-Track.y(from));for(int d=0;d<8;d++)if(Track.DX[d]==x&&Track.DY[d]==y)return d;return -1;}
    private static int angle(int a,int b){int difference=(b-a+8)&7;return Math.min(difference,8-difference);}
    private static Result fail(Failure failure,int pos){return new Result(Map.of(),0,failure,pos);}
    private SmartTrack(){}
}
