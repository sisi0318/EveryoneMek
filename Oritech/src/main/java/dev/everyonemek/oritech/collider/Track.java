package dev.everyonemek.oritech.collider;

import java.util.*;

/** A per-machine, world-independent beamline. Distances are virtual blocks, speed is blocks/s. */
public final class Track {
    public static final int SIZE=64,MAX_CELLS=SIZE*SIZE,RING=1,MOTOR=2,SENSOR=3,A=4,B=5;
    public static final int[] DX={1,1,0,-1,-1,-1,0,1},DY={0,1,1,1,0,-1,-1,-1};
    public record Node(int kind,int direction,int bend){
        public Node {if(kind<RING||kind>B||direction<0||direction>7||bend<0||bend>2)throw new IllegalArgumentException("node");}
        public int front(){return (direction+(bend==1?7:bend==2?1:0))&7;}
        public int back(){return (direction+4)&7;}
        public int exit(int incoming){
            if(kind==MOTOR||kind==SENSOR)return incoming;
            int entered=(incoming+4)&7;
            if(entered==back())return front();
            if(entered==front())return back();
            return -1;
        }
    }
    public record Rules(int maxGap,double bendFactor,long accelerationCost){
        public Rules {maxGap=Math.clamp(maxGap,1,SIZE);bendFactor=Double.isFinite(bendFactor)?Math.max(0,bendFactor):0;accelerationCost=Math.max(0,accelerationCost);}
        public double gap(long speed){return Math.min(maxGap,Math.max(2,Math.sqrt(Math.max(1,speed))/2));}
        public double bend(long speed){return bendFactor>0?Math.sqrt(Math.max(1,speed))/bendFactor:Double.POSITIVE_INFINITY;}
    }
    public enum Fault { NONE,MISSING_GATE,WRONG_FACING,NO_MEETING,NO_MOTOR,BEND_TOO_TIGHT,GAP_TOO_WIDE,INVALID_LAYOUT }
    public record Segment(int from,int to,int direction,double length,int nextDirection,Node target){}
    public record Plan(List<Segment> segments,int loopStart,Fault fault,int problem){
        public boolean valid(){return fault==Fault.NONE;}
        public int next(int index){return index+1<segments.size()?index+1:loopStart;}
    }
    public static int cell(int x,int y){return x+y*SIZE;}
    public static int x(int cell){return cell%SIZE;}
    public static int y(int cell){return cell/SIZE;}
    public static boolean inBounds(int value){return value>=0&&value<MAX_CELLS;}
    public static int pack(int pos,Node node){return pos|(node.kind()<<12)|(node.direction()<<15)|(node.bend()<<18);}
    public static Node unpack(int packed){return new Node((packed>>12)&7,(packed>>15)&7,(packed>>18)&3);}

    public static Plan plan(Map<Integer,Node> cells,Rules rules){
        int a=-1,b=-1;
        for(var entry:cells.entrySet()){
            if(!inBounds(entry.getKey()))return invalid(Fault.INVALID_LAYOUT,entry.getKey());
            if(entry.getValue().kind()==A){if(a>=0)return invalid(Fault.INVALID_LAYOUT,entry.getKey());a=entry.getKey();}
            if(entry.getValue().kind()==B){if(b>=0)return invalid(Fault.INVALID_LAYOUT,entry.getKey());b=entry.getKey();}
        }
        if(a<0||b<0)return invalid(Fault.INVALID_LAYOUT,-1);
        int at=a,direction=cells.get(a).direction();var segments=new ArrayList<Segment>();var visited=new HashMap<Integer,Integer>();boolean meeting=false;
        for(int step=0;step<MAX_CELLS*8;step++){
            int key=at*8+direction;
            if(visited.containsKey(key))return meeting?new Plan(List.copyOf(segments),visited.get(key),Fault.NONE,-1):invalid(Fault.NO_MEETING,at);
            visited.put(key,segments.size());int next=-1;
            for(int distance=1;distance<=rules.maxGap();distance++){
                int nx=x(at)+DX[direction]*distance,ny=y(at)+DY[direction]*distance;
                if(nx<0||ny<0||nx>=SIZE||ny>=SIZE)break;
                int candidate=cell(nx,ny);if(cells.containsKey(candidate)){next=candidate;break;}
            }
            if(next<0)return invalid(Fault.MISSING_GATE,at);
            var node=cells.get(next);int out=node.exit(direction);
            if(out<0)return invalid(Fault.WRONG_FACING,next);
            if(node.kind()==B){if(node.direction()!=((direction+4)&7))return invalid(Fault.WRONG_FACING,next);meeting=true;}
            segments.add(new Segment(at,next,direction,Math.hypot(x(next)-x(at),y(next)-y(at)),out,node));
            at=next;direction=out;
        }
        return invalid(Fault.INVALID_LAYOUT,at);
    }
    private static Plan invalid(Fault fault,int pos){return new Plan(List.of(),-1,fault,pos);}

    public record Validation(Fault fault,int problem,boolean collision){public boolean valid(){return fault==Fault.NONE&&collision;}}
    private static final class Probe {long speed=1;double bend=15000,previous=15000;}
    /** Check the first and last required laps. Intermediate laps only relax gaps and tighten bends. */
    public static Validation preflight(Plan plan,Rules rules,long required){
        if(!plan.valid())return new Validation(plan.fault(),plan.problem(),false);
        var probe=new Probe();var first=probe(plan,0,probe,rules,required);if(first.fault()!=Fault.NONE||first.collision())return first;
        int meeting=-1,motors=0,beforeMeeting=0;
        for(int i=plan.loopStart();i<plan.segments().size();i++){var node=plan.segments().get(i).target();if(node.kind()==B&&meeting<0){meeting=i;beforeMeeting=motors;}if(node.kind()==MOTOR)motors++;}
        if(meeting<0)return new Validation(Fault.NO_MEETING,plan.segments().getLast().to(),false);
        if(motors==0)return new Validation(Fault.NO_MOTOR,plan.segments().get(meeting).to(),false);
        // This warm-up makes both bend distances independent of any leading path into the loop.
        var warm=probe(plan,plan.loopStart(),probe,rules,required);if(warm.fault()!=Fault.NONE||warm.collision())return warm;
        long missing=Math.max(0,required-1-probe.speed-beforeMeeting),skip=(missing+motors-1)/motors;
        if(skip>0){
            probe.speed=Math.min(Integer.MAX_VALUE,probe.speed+(skip-1)*motors);
            var last=probe(plan,plan.loopStart(),probe,rules,required);if(last.fault()!=Fault.NONE||last.collision())return last;
        }
        return probe(plan,plan.loopStart(),probe,rules,required);
    }
    private static Validation probe(Plan plan,int start,Probe p,Rules rules,long required){
        for(int i=start;i<plan.segments().size();i++){var s=plan.segments().get(i);
            if(Math.max(Math.abs(x(s.to())-x(s.from())),Math.abs(y(s.to())-y(s.from())))>rules.gap(p.speed)+1e-7)return new Validation(Fault.GAP_TOO_WIDE,s.from(),false);
            p.bend+=s.length();if(s.target().kind()==B&&p.speed+1>=required)return new Validation(Fault.NONE,-1,true);
            if(s.direction()!=s.nextDirection()){
                if(p.bend+p.previous<=rules.bend(p.speed))return new Validation(Fault.BEND_TOO_TIGHT,s.to(),false);
                p.previous=p.bend;p.bend=0;
            }
            if(s.target().kind()==MOTOR&&p.speed<Integer.MAX_VALUE)p.speed++;
        }
        return new Validation(Fault.NONE,-1,false);
    }

    /** Mutable progress is owned only by one collider and is serialized with its reserved ingredients. */
    public static final class Beam {
        public int segment;
        public double offset,bendDistance=15000,previousBend=15000;
        public long speed=1;
        public double x(Plan plan){var s=plan.segments().get(segment);return Track.x(s.from())+DX[s.direction()]*offset/(s.direction()%2==0?1:Math.sqrt(2));}
        public double y(Plan plan){var s=plan.segments().get(segment);return Track.y(s.from())+DY[s.direction()]*offset/(s.direction()%2==0?1:Math.sqrt(2));}
        public boolean valid(Plan p){return p.valid()&&segment>=0&&segment<p.segments().size()&&Double.isFinite(offset)&&offset>=0&&offset<=p.segments().get(segment).length()+1e-7
            &&Double.isFinite(bendDistance)&&bendDistance>=0&&Double.isFinite(previousBend)&&previousBend>=0&&speed>=1&&speed<=Integer.MAX_VALUE;}
    }
    public record Step(long spent,boolean collision,boolean needsPower,Fault fault,int problem){}

    /** Pay at each real virtual motor; a power shortage preserves the beam at that motor. */
    public static Step advance(Plan plan,Beam beam,Rules rules,long available,long required){
        if(!beam.valid(plan))return new Step(0,false,false,Fault.INVALID_LAYOUT,-1);
        long spent=0;double distance=beam.speed/20.0;
        for(int steps=0;steps<2048;steps++){
            var s=plan.segments().get(beam.segment);
            if(beam.offset==0&&Math.max(Math.abs(x(s.to())-x(s.from())),Math.abs(y(s.to())-y(s.from())))>rules.gap(beam.speed)+1e-7)return new Step(spent,false,false,Fault.GAP_TOO_WIDE,s.from());
            double move=Math.min(Math.max(0,s.length()-beam.offset),distance);
            beam.offset+=move;beam.bendDistance+=move;distance-=move;
            if(beam.offset+1e-7<s.length())break;
            // The second emitter is fired exactly when A reaches it, in the opposite direction.
            // Its initial speed is 1, so the real relative collision speed is A + 1.
            if(s.target().kind()==B&&beam.speed+1>=required)return new Step(spent,true,false,Fault.NONE,s.to());
            boolean turn=s.direction()!=s.nextDirection();
            if(turn&&beam.bendDistance+beam.previousBend<=rules.bend(beam.speed))return new Step(spent,false,false,Fault.BEND_TOO_TIGHT,s.to());
            long cost=s.target().kind()==MOTOR?saturatedMultiply(beam.speed,rules.accelerationCost()):0;
            if(cost>Math.max(0,available-spent))return new Step(spent,false,true,Fault.NONE,s.to());
            spent+=cost;
            if(turn){beam.previousBend=beam.bendDistance;beam.bendDistance=0;}
            if(s.target().kind()==MOTOR&&beam.speed<Integer.MAX_VALUE)beam.speed++;
            int next=plan.next(beam.segment);if(next<0)return new Step(spent,false,false,Fault.MISSING_GATE,s.to());
            beam.segment=next;beam.offset=0;
            if(distance<1e-7)break;
        }
        return new Step(spent,false,false,Fault.NONE,-1);
    }
    private static long saturatedMultiply(long a,long b){return b>0&&a>Long.MAX_VALUE/b?Long.MAX_VALUE:a*b;}
    private Track(){}
}
